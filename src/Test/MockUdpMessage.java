package Test;

import Replicas.Replica3.campus.UdpMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class MockUdpMessage {
    static String udpTest(int serverPort) {
        System.out.println("sending message");
        DatagramSocket aSocket = null;
        String res = "";
        try {
            aSocket = new DatagramSocket();

            String[] timeSlots = {"17:00-21:00", "18:00", "19:00", "20:00"};

            UdpMessage myMessage = new UdpMessage(
                    "bookRoom", "ID", "DVL", "101", "10:00-11:00", "01-11-2021", null);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutput oo = new ObjectOutputStream(bStream);
            oo.writeObject(myMessage);
            oo.close();

//            byte[] m = msg.getBytes();
            byte[] m = bStream.toByteArray();
            InetAddress aHost = InetAddress.getByName("localhost");

            DatagramPacket request = new DatagramPacket(m, m.length, aHost, serverPort);
            aSocket.send(request);

            byte[] buffer = new byte[4096];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            aSocket.receive(reply);
            res += new String(reply.getData());

        } catch (SocketException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return res.trim();
    }
}
