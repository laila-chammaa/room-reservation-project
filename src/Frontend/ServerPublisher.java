package Frontend;

import javax.xml.ws.Endpoint;

public class ServerPublisher {
    public static void main(String[] args) {
        FrontendImpl frontend = new FrontendImpl();

        Endpoint ep1 = Endpoint.create(frontend);
        ep1.publish("http://127.0.0.1:8080/Campus");
    }
}


