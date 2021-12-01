package Replicas.Replica2;

import DRRS.Config;
import DRRS.Replica;
import DRRS.ReplicaPorts;
import org.json.simple.JSONObject;

public class Replica2 extends Replica {
	
	private static final ReplicaPorts ports = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(2);
	
	public Replica2() {
		super(new RoomRecordCampus("DVL", ports.getDvlPort()));
	}
	
	@Override
	public void startServers() {
	
	}
	
	@Override
	public void stopServers() {
	
	}
	
	@Override
	public JSONObject getCurrentData() {
		return null;
	}
	
	@Override
	public void setCurrentData(JSONObject currentData) {
	
	}
}
