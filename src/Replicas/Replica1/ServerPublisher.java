package Replicas.Replica1;

import Replicas.Replica1.com.CampusServer;
import Replicas.Replica1.model.CampusID;

import javax.xml.ws.Endpoint;
import java.util.HashMap;

public class ServerPublisher {
    public static void main(String[] args) {

        HashMap<String, String> serverDetails = new HashMap<>();
        serverDetails.put("KKL", "localhost:30100");
        serverDetails.put("DVL", "localhost:30200");
        serverDetails.put("WST", "localhost:30300");

        CampusServer serverKKL = new CampusServer(CampusID.KKL, "localhost", 30100, serverDetails);
        CampusServer serverDVL = new CampusServer(CampusID.DVL, "localhost", 30200, serverDetails);
        CampusServer serverWST = new CampusServer(CampusID.WST, "localhost", 30300, serverDetails);

        Endpoint ep1 = Endpoint.create(serverKKL);
        ep1.publish("http://127.0.0.1:8080/KKL");

        Endpoint ep2 = Endpoint.create(serverDVL);
        ep2.publish("http://127.0.0.1:8080/DVL");

        Endpoint ep3 = Endpoint.create(serverWST);
        ep3.publish("http://127.0.0.1:8080/WST");

    }
}


