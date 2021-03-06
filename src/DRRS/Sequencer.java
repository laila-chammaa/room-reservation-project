package DRRS;

import Frontend.Message;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
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
    private final ConcurrentHashMap<Long, AtomicLong> lastAckedSeqNums;

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
        System.out.println("Listening for messages.");
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        InetAddress address = packet.getAddress();
        int port = packet.getPort();

        String received = new String(packet.getData(), 0, packet.getLength());
        
        System.out.println("Received: " + received);

        // Process input -- this is a Json Object
        JSONObject jsonObject = (JSONObject) (JSONValue.parse(received.trim()));

        // Incoming message could be an ack, check if this is the case
        if (!jsonObject.containsKey(MessageKeys.COMMAND_TYPE)) {
            System.out.println("Error: expected incoming message to have field " + MessageKeys.COMMAND_TYPE);
            System.out.println("Ignoring this message");
            return;
        }

        String messageType = (String) (jsonObject.get(MessageKeys.COMMAND_TYPE));
        if (messageType.equals(Config.ACK)) {
            // Received an ack from a member, checking for existence of fields rm_port_num and sequence_number

            if (!jsonObject.containsKey(MessageKeys.RM_PORT_NUMBER)) {
                System.out.println("Error: expected incoming ack to have field " + MessageKeys.RM_PORT_NUMBER);
                System.out.println("Ignoring this message");
                return;
            }

            if (!jsonObject.containsKey(MessageKeys.SEQ_NUM)) {
                System.out.println("Error: expected incoming ack to have field " + MessageKeys.SEQ_NUM);
                System.out.println("Ignoring this message");
                return;
            }

            long portNb = (Long) jsonObject.get(MessageKeys.RM_PORT_NUMBER);
            Object lastSeqNum = jsonObject.get(MessageKeys.SEQ_NUM);
            if (lastSeqNum instanceof Long) {
                processAck((Long) lastSeqNum, portNb);
            }
            // If the sequence is in the sync phase, no further messages are accepted from the FE until the history buffer is clear
        } else if (running) {
            // Get the sequence id from the message to send in the ack message

            Object seqId = jsonObject.get(MessageKeys.MESSAGE_ID);
            if (seqId == null) {
                System.out.println("Error: received message without a seqId field");
            } else {
                long seqIdAsLong = (long) seqId;

                // Send acknowledgement from the frontend
                sendAckToFE(seqIdAsLong, address, SEQ_FE);

                // check if message is a duplicate (e.g. due to retransmission by the FE)
                for (JSONObject nextElement : deliveryQueue) {
                    Object messageId = nextElement.get(MessageKeys.MESSAGE_ID);
                    if (messageId instanceof Long && (Long) messageId == seqIdAsLong) {
                        System.out.println("Message w/ message ID " + messageId + " was already delivered, sent ack only");
                        return;
                    }
                }

                // If message is not a duplicate, push to the history buffer and multicast
                System.out.println("Received from FE " + address + ":" + port + ", jsonObject " + jsonObject.toJSONString());

                // increment sequence number atomically and append it to the message
                long seqNum = sequenceNumber.incrementAndGet();
                jsonObject.put(MessageKeys.SEQ_NUM, seqNum);

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
        DatagramPacket packetToSend = new DatagramPacket(msgJson.getBytes(), msgJson.length(), multicastGroupAddress, SEQ_RE);
        multicastSocket.send(packetToSend);
    }

    void processAck(long ackSeqNum, long portNb) {
        if (!lastAckedSeqNums.containsKey(portNb)) {
            System.out.println("Replica " + portNb + " new last Acked SeqNum: " + ackSeqNum);
            lastAckedSeqNums.put(portNb, new AtomicLong(ackSeqNum));
        } else {
            System.out.println("Replica " + portNb + " new last Acked SeqNum: " + ackSeqNum);
            lastAckedSeqNums.get(portNb).set(ackSeqNum);

            // Check if history can be cleaned, i.e. whether any messages in the queue have been acked by every replica
            long minSeqNum = lastAckedSeqNums.values().stream()
                    .min(Comparator.comparingLong(AtomicLong::longValue))
                    .orElse(new AtomicLong()).longValue();

            // Remove elements from the head of the queue (oldest elements in the history first), until the sequence ID
            // encountered is larger than the minimum sequence number across the replica
            for (JSONObject jsonObject : deliveryQueue) {
                if ((Long) jsonObject.get(MessageKeys.SEQ_NUM) > minSeqNum) {
                    break;
                } else {
                    deliveryQueue.removeFirst();
                }
            }
        }

    }

    void sendAckToFE(long ackSeqNum, InetAddress address, int port) {
        JSONObject ackObject = new JSONObject();
        ackObject.put(MessageKeys.COMMAND_TYPE, Config.ACK);
        ackObject.put(MessageKeys.SEQ_NUM, ackSeqNum);
        byte[] ackData = ackObject.toString().getBytes();
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
