package Replicas.Replica4.udp;


import Replicas.Replica4.com.CampusServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPServer implements Runnable {
    private String UDPHost;
    private int UDPPort;
    private CampusServer campusServer;
    boolean connectionStatus;

    public UDPServer(String UDPHost, int UDPPort, CampusServer campusServer) {
        this.UDPHost = UDPHost;
        this.UDPPort = UDPPort;
        this.campusServer = campusServer;
        connectionStatus = true;
    }

    public void start() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(UDPPort);

            byte[] dataBuffer = new byte[1048];

            while (connectionStatus) {
                //1. Create a datagram for incoming packets
                DatagramPacket requestPacket = new DatagramPacket(dataBuffer, dataBuffer.length);

                //2. The server will take the incoming request
                serverSocket.receive(requestPacket);

                //3. From the packet, we take the necessary information for reply
                InetAddress ip = requestPacket.getAddress();
                int requestPort = requestPacket.getPort();

                //4. Translate the byte data from the request to invoke the method
                CampusUDPInterface requestData = MarshallService.unmarshall(requestPacket.getData());
                requestData.execute(this.campusServer, this.campusServer.getCampusID());

                //5. Reply
                byte[] reply = MarshallService.marshall(requestData);
                DatagramPacket replyPacket = new DatagramPacket(reply, reply.length, ip, requestPort);
                serverSocket.send(replyPacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        connectionStatus = false;
    }

    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
