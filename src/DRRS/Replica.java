package DRRS;

import Replicas.CampusServerInterface;

public class Replica {
	
	private CampusServerInterface dvlCampus;
	private CampusServerInterface kklCampus;
	private CampusServerInterface wstCampus;
	
	Replica(CampusServerInterface dvlCampus, CampusServerInterface kklCampus, CampusServerInterface wstCampus) {
		this.dvlCampus = dvlCampus;
		this.kklCampus = kklCampus;
		this.wstCampus = wstCampus;
	}
	
	public void reset() {
	
	}
	
	private void getCurrentValidData() {
		// Reaching to the other replicas to get their data
	}
}
