package DRRS;

import Frontend.Message;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import static DRRS.Config.IPAddresses.MULTICAST_ADR;
import static DRRS.Config.PortNumbers.*;

public class Sequencer extends Thread{
    private static Sequencer sequencer;

    private final InetAddress multicastGroupAddress;
    private final AtomicLong sequenceNumber;
    private final ConcurrentHashMap<String, AtomicLong> lastAckedSeqNums;

    ConcurrentLinkedDeque<JSONObject> deliveryQueue;

    private final DatagramSocket socket;
    private final MulticastSocket multicastSocket;
    private boolean running;
    private final byte[] buf;

    public static Sequencer getInstance() throws IOException {
        if (sequencer == null) {
            sequencer = new Sequencer();
        }
        return sequencer;
    }

    public Sequencer() throws IOException {
        sequenceNumber = new AtomicLong();
        deliveryQueue = new ConcurrentLinkedDeque<>();

        socket = new DatagramSocket(FE_SEQ);

        // Setup multicast address
        multicastSocket = new MulticastSocket(SEQ_RE);
        multicastGroupAddress = InetAddress.getByName(MULTICAST_ADR);
        multicastSocket.joinGroup(multicastGroupAddress);

        lastAckedSeqNums = new ConcurrentHashMap<>();

        running = false;
        buf = new byte[2 << 10];
    }

    void listenForMessages() throws IOException {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        InetAddress address = packet.getAddress();
        int port = packet.getPort();

        String received = new String(packet.getData(), 0, packet.getLength());

        // Process input -- this is a Json Object
        JSONObject jsonObject = (JSONObject) (JSONValue.parse(received.trim()));

        // Incoming message could be an ack, check if this is the case
        String messageType = (String) (jsonObject.get(MessageKeys.COMMAND_TYPE));
        if (messageType.equals(Config.ACK)) {
            // Received an ack from a member
            // TODO: The last sequence number acked by a member should be in this message
            Object lastSeqNum = jsonObject.get("seqNum");
            if (lastSeqNum instanceof Long) {
                processAck((Long) lastSeqNum, address.toString());
            }
            // If the sequence is in the sync phase, no further messages are accepted from the FE until the history buffer is clear
        } else if (running) {
            // Get the sequence id from the message to send in the ack message
            Object seqId = jsonObject.get(MessageKeys.MESSAGE_ID);
            if (seqId == null) {
                System.out.println("Error: received message without a seqId field");
            } else {
                long seqIdAsLong = (long) seqId;

                sendAckToFE(seqIdAsLong, address, SEQ_FE);

                // check if message is a duplicate (e.g. due to retransmission by the FE)
                for (JSONObject nextElement : deliveryQueue) {
                    Object messageId = nextElement.get(MessageKeys.MESSAGE_ID);
                    if (messageId instanceof Long && (Long) messageId == seqIdAsLong) {
                        System.out.println("Message was already delivered, sent ack only");
                        return;
                    }
                }

                // If message is not a duplicate, push to the history buffer and multicast
                System.out.println("Received from FE " + address + ":" + port + ", jsonObject " + jsonObject.toJSONString());

                // increment sequence number atomically and append it to the message
                long seqNum = sequenceNumber.incrementAndGet();
                jsonObject.put("seq", seqNum);

                // Store message in the history buffer
                deliveryQueue.add(jsonObject);

                // If the history buffer reaches its maximum size, switch to sync phase
            if (deliveryQueue.size() > Config.DELIVERY_QUEUE_MAX_SIZE) {
                running = false;
            }

                // Transmit the message to all destinations
                multicastMessage(jsonObject);
            }
        }
    }

    void multicastMessage(JSONObject msg) throws IOException {
        String msgJson = msg.toJSONString();
        DatagramPacket packetToSend = new DatagramPacket(msgJson.getBytes(), msgJson.length(), multicastGroupAddress, SEQ_FE);
        multicastSocket.send(packetToSend);

        // TODO: in Replica manager, make sure the sequencer is sending messages in sequential order
    }

    void processAck(long ackSeqNum, String memberName) {
        if (!lastAckedSeqNums.containsKey(memberName)) {
            System.out.println("Replica " + memberName + " new last Acked SeqNum: " + ackSeqNum);
            lastAckedSeqNums.put(memberName, new AtomicLong(ackSeqNum));
        } else {
            System.out.println("Replica " + memberName + " new last Acked SeqNum: " + ackSeqNum);
            lastAckedSeqNums.get(memberName).set(ackSeqNum);

            // Check if history can be cleaned, i.e. whether any messages in the queue have been acked by every replica
            long minSeqNum = lastAckedSeqNums.values().stream()
                    .min(Comparator.comparingLong(AtomicLong::longValue))
                    .orElse(new AtomicLong()).longValue();

            // Remove elements from the head of the queue (oldest elements in the history first), until the sequence ID
            // encountered is larger than the minimum sequence number across the replica
            for (JSONObject jsonObject : deliveryQueue) {
                if ((Integer) jsonObject.get("seq") > minSeqNum) {
                    break;
                } else {
                    deliveryQueue.removeFirst();
                }
            }
        }

    }

    void sendAckToFE(long ackSeqNum, InetAddress address, int port) {
        byte[] ackData = Long.toString(ackSeqNum).getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(ackData, ackData.length, address, port);
        try {
            System.out.println("Sending out an ack to " + address.getHostAddress() + ":" + port + " with seqNum " + ackSeqNum);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void resend(Message message) {

    }

    @Override
    public void run() {
        running = true;
        while(true) {
            try {
                listenForMessages();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Exiting sequencer thread");
                break;
            }
        }
    }

    public static void main(String[] args) {
        Sequencer sequencer = null;
        try {
            sequencer = Sequencer.getInstance();
            sequencer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
