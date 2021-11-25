package DRRS.config;

import java.util.HashMap;
import java.util.Map;

public class Ports {
	
	private static final ReplicaManagerPorts ReplicaManager1 = new ReplicaManagerPorts(4001, 5001, 5002, 5003);
	private static final ReplicaManagerPorts ReplicaManager2 = new ReplicaManagerPorts(4002, 5004, 5005, 5006);
	private static final ReplicaManagerPorts ReplicaManager3 = new ReplicaManagerPorts(4003, 5007, 5008, 5009);
	private static final ReplicaManagerPorts ReplicaManager4 = new ReplicaManagerPorts(4004, 5010, 5011, 5012);
	
	public static final Map<Integer, ReplicaManagerPorts> ReplicaManagerPortMap = new HashMap<>() {{
		put(1, ReplicaManager1); put(2, ReplicaManager2); put(3, ReplicaManager3); put(4, ReplicaManager4);
	}};
	
	// TODO: Define frontend/sequencer port numbers
}
