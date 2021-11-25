package DRRS;

import DRRS.config.Ports;
import DRRS.config.ReplicaManagerPorts;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.Queue;

public class ReplicaManager {
	private int failureCount;
	private Queue<String> requestQueue;
	private Replica replica;
	private final ReplicaManagerPorts replicaManagerPorts;
	private Thread replicaThread;
	
	public ReplicaManager(int replicaNumber) {
		this.replicaManagerPorts = Ports.ReplicaManagerPortMap.get(replicaNumber);
		
		if (this.replicaManagerPorts == null) {
			throw new IllegalArgumentException("Incorrect replica number: " + replicaNumber);
		}
		
		replicaThread = new Thread(new ReplicaThread());
	}
	
	class ReplicaThread implements Runnable {
		@Override
		public void run() {
			try (DatagramSocket socket = new DatagramSocket(replicaManagerPorts.getRmPort())) {
				while(true) {
					// Add request logic
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void start() {
		this.replicaThread.start();
	}
	
	public void stop() throws InterruptedException {
		this.replicaThread.join();
	}
	
	public void resetReplica() throws InterruptedException {
		this.replica.reset();
		this.replicaThread.join();
	}
}
