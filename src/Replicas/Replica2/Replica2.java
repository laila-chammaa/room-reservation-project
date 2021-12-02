package Replicas.Replica2;

import DRRS.Config;
import DRRS.Replica;
import DRRS.ReplicaPorts;

import java.io.IOException;
import java.util.HashMap;

public class Replica2 extends Replica {
	
	private static final ReplicaPorts ports = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(2);

	public Replica2() throws IOException {
		super(
				new RoomRecordCampus("DVL", ports.getDvlPort(), new HashMap<String, Integer>(){{ put("KKL", ports.getKklPort()); put("WST", ports.getWstPort()); }}),
				new RoomRecordCampus("KKL", ports.getKklPort(), new HashMap<String, Integer>(){{ put("DVL", ports.getDvlPort()); put("WST", ports.getWstPort()); }}),
				new RoomRecordCampus("WST", ports.getWstPort(), new HashMap<String, Integer>(){{ put("DVL", ports.getDvlPort()); put("KKL", ports.getKklPort()); }})
		);
		dvlThread = new Thread((RoomRecordCampus)dvlCampus);
		kklThread = new Thread((RoomRecordCampus)kklCampus);
		wstThread = new Thread((RoomRecordCampus)wstCampus);
	}
}
