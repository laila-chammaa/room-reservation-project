package DRRS;

public class ReplicaPorts {
	private final int rmPort;
	private final int dvlPort;
	private final int kklPort;
	private final int wstPort;
	private final String ipAddress;
	
	public ReplicaPorts(int rmPort, int dvlPort, int kklPort, int wstPort, String ipAddress) {
		this.rmPort = rmPort;
		this.dvlPort = dvlPort;
		this.kklPort = kklPort;
		this.wstPort = wstPort;
		this.ipAddress = ipAddress;
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
	
	public String getRmIpAddress() {
		return ipAddress;
	}
}
