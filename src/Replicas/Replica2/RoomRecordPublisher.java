package Replicas.Replica2;

import java.io.IOException;
import java.util.HashMap;
import javax.xml.ws.Endpoint;

public class RoomRecordPublisher {
	public static void main(String[] args) throws IOException {
		String dvl = "DVL";
		String kkl = "KKL";
		String wst = "WST";
		int socket1 = 8001;
		int socket2 = 8002;
		int socket3 = 8003;
		
		System.out.println("Initiating campus services...");
		
		// Creating campus services
		RoomRecordCampus dvlCampus = new RoomRecordCampus();
		dvlCampus.init(dvl, socket1, new HashMap<String, Integer>(){{ put(kkl, socket2); put(wst, socket3); }});
		RoomRecordCampus kklCampus = new RoomRecordCampus();
		kklCampus.init(kkl, socket2, new HashMap<String, Integer>(){{ put(dvl, socket1); put(wst, socket3); }});
		RoomRecordCampus wstCampus = new RoomRecordCampus();
		wstCampus.init(wst, socket3, new HashMap<String, Integer>(){{ put(dvl, socket1); put(kkl, socket2); }});
		
		new Thread(dvlCampus).start();
		new Thread(kklCampus).start();
		new Thread(wstCampus).start();
		
		// Publishing endpoints
		Endpoint dvlEndpoint = Endpoint.create(dvlCampus);
		dvlEndpoint.publish("http://localhost:8081/dvl/");
		Endpoint kklEndpoint = Endpoint.create(kklCampus);
		kklEndpoint.publish("http://localhost:8081/kkl/");
		Endpoint wstEndpoint = Endpoint.create(wstCampus);
		wstEndpoint.publish("http://localhost:8081/wst/");
		
		System.out.println("Campus services published.");
	}
}
