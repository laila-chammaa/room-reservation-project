package DRRS;

import Frontend.Message;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import static DRRS.Config.IPAddresses.MULTICAST_ADR;
import static DRRS.Config.PortNumbers.*;

public class Sequencer extends Thread{
    private static Sequencer sequencer;

    private final InetAddress multicastGroupAddress;
    AtomicInteger sequenceNumber;
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
        sequenceNumber = new AtomicInteger();
        deliveryQueue = new ConcurrentLinkedDeque<>();

        socket = new DatagramSocket(FE_SEQ);

        // Setup multicast address
        multicastSocket = new MulticastSocket(SEQ_RE);
        multicastGroupAddress = InetAddress.getByName(MULTICAST_ADR);
        multicastSocket.joinGroup(multicastGroupAddress);

        running = false;
        buf = new byte[10 << 2];
    }

    void listenForMessages() throws IOException {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        InetAddress address = packet.getAddress();
        int port = packet.getPort();

        String received = new String(packet.getData(), 0, packet.getLength());

        // Process input -- this is a Json Object
        JSONObject jsonObject = (JSONObject) (JSONValue.parse(received.trim()));

        // Get the sequence id from the message to send in the ack message
        Object seqId = jsonObject.get(MessageKeys.MESSAGE_ID);
        if (seqId == null) {
            System.out.println("Error: received message without a seqId field");
        } else {
            long seqIdAsLong = (long) seqId;

            // check if message is a duplicate (e.g. due to retransmission by the FE)
            sendAckToFE(seqIdAsLong, address, SEQ_FE);

            if (deliveryQueue.contains(seqIdAsLong)) {
                System.out.println("Message was already delivered, sending ack");
                return;
            }

            System.out.println("Received from FE " + address + ":" + port + ", jsonObject " + jsonObject.toJSONString());

            // increment sequence number atomically and append it to the message
            int seqNum = sequenceNumber.incrementAndGet();
            jsonObject.put("seq", seqNum);

            // Store message in the history buffer
            // TODO: Manage the queue
            deliveryQueue.add(jsonObject);

            // Transmit the message to all destinations
            multicastMessage(jsonObject);
        }

    }

    void multicastMessage(JSONObject msg) throws IOException {
        String msgJson = msg.toJSONString();
        DatagramPacket packetToSend = new DatagramPacket(msgJson.getBytes(), msgJson.length(), multicastGroupAddress, SEQ_FE);
        multicastSocket.send(packetToSend);

        // TODO: in Replica manager, make sure the sequencer is sending messages in sequential order
    }

    void processAck(int ackSeqNum) {

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
