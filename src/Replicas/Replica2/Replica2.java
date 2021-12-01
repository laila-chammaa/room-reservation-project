package Replicas.Replica2;

import DRRS.Config;
import DRRS.Replica;
import DRRS.ReplicaPorts;
import org.json.simple.JSONObject;

public class Replica2 implements Replica {
	
	RoomRecordCampus dvlCampus;
	RoomRecordCampus kklCampus;
	RoomRecordCampus wstCampus;
	
	public Replica2() {
		ReplicaPorts ports = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(2);
		dvlCampus = new RoomRecordCampus("DVL", ports.getDvlPort());
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
	
	@Override
	public void executeRequest(JSONObject request) {
	
	}
}
