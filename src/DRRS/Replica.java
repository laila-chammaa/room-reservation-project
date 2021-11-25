package DRRS;

import Replicas.CampusServerInterface;

public class Replica {
	
	private Thread dvlThread;
	private Thread kklThread;
	private Thread wstThread;
	
	Replica(CampusServerInterface dvlCampus, CampusServerInterface kklCampus, CampusServerInterface wstCampus) {
		this.dvlThread = new Thread(dvlCampus);
		this.kklThread = new Thread(kklCampus);
		this.wstThread = new Thread(wstCampus);
	}
	
	public void reset() {
	
	}
	
	private void getCurrentValidData() {
		// Reaching to the other replicas to get their data
	}
}
