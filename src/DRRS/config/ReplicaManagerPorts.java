package DRRS.config;

public class ReplicaManagerPorts {
	private final int rmPort;
	private final int dvlPort;
	private final int kklPort;
	private final int wstPort;
	
	public ReplicaManagerPorts(int rmPort, int dvlPort, int kklPort, int wstPort) {
		this.rmPort = rmPort;
		this.dvlPort = dvlPort;
		this.kklPort = kklPort;
		this.wstPort = wstPort;
	}
	
	public int getRmPort() {
		return rmPort;
	}
	
	public int getDvlPort() {
		return dvlPort;
	}
	
	public int getKklPort() {
		return kklPort;
	}
	
	public int getWstPort() {
		return wstPort;
	}
}
