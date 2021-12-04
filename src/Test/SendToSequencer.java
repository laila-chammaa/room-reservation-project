package Test;

import DRRS.Config;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SendToSequencer {
    public JSONObject sendMessage(JSONObject msgJson) {
        org.json.simple.parser.JSONParser parser = new JSONParser();

        DatagramSocket senderSocket = null;
        DatagramSocket receiverSocket = null;

        try {
            senderSocket = new DatagramSocket();
            receiverSocket = new DatagramSocket(Config.PortNumbers.SEQ_FE);

            DatagramSocket datagramSocket =  new DatagramSocket(Config.PortNumbers.RE_FE);;

            byte[] messageBuffer = msgJson.toJSONString().getBytes();
            InetAddress host = InetAddress.getByName(Config.IPAddresses.SEQUENCER);

            DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, Config.PortNumbers.FE_SEQ);

            senderSocket.send(request);

            while (true) {
                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                receiverSocket.receive(reply);

//                String replyString = new String(reply.getData(), reply.getOffset(), reply.getLength());
//                JSONObject jsonMessage = (JSONObject) parser.parse(replyString);
//
//                System.out.println(jsonMessage);

                datagramSocket.receive(reply);
                String data = new String(reply.getData()).trim();
                JSONObject jsonMessage2 = (JSONObject) parser.parse(data);

                System.out.println(jsonMessage2);

                if (jsonMessage2 != null && (jsonMessage2.get("status_code").toString().equals("SUCCESS")
                        || jsonMessage2.get("status_code").toString().equals("FAIL")) && jsonMessage2.get("message_id").toString().equals(msgJson.get("message_id").toString())) {

                    datagramSocket.close();
                    senderSocket.close();
                    receiverSocket.close();

                    return jsonMessage2;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return (JSONObject) new JSONObject().put("status_code", "ERROR");
    }
}