package DRRS;

import DRRS.config.Config;
import DRRS.config.ReplicaPorts;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ReplicaManager {
	private static final int MAX_FAILURE_COUNT = 3;
	private int failureCount;
	private Queue<JSONObject> requestQueue;
	private final Replica replica;
	private final ReplicaPorts replicaManagerPorts;
	private final Object queueLock;
	private Thread managerThread;
	private InetAddress managerAddress;
	private JSONParser parser = new JSONParser();
	
	public ReplicaManager(int replicaNumber, Replica replica) throws UnknownHostException {
		this.replicaManagerPorts = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(replicaNumber);
		managerAddress = InetAddress.getByName(replicaManagerPorts.getRmIpAddress());
		
		this.replica = replica;
		managerThread = new Thread(new ManagerThread());
		this.requestQueue = new LinkedBlockingQueue<>();
		this.queueLock = new Object();
	}
	
	class ManagerThread implements Runnable {
		@Override
		public void run() {
			try (DatagramSocket socket = new DatagramSocket(replicaManagerPorts.getRmPort())) {
				while(true) {
					byte[] receivedBytes = new byte[1024];
					DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, receivedBytes.length);
					socket.receive(receivedPacket);
					process(socket, receivedPacket);
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void process(DatagramSocket socket, DatagramPacket request) throws ParseException {
		JSONObject message = (JSONObject) parser.parse(new String(request.getData()).trim());
		
		if (message.equals("getData")) {
			try {
				JSONObject data = replica.getCurrentData();
				byte[] dataSent = data.toString().getBytes();
				InetAddress ipAddress = request.getAddress();
				int port = request.getPort();
				DatagramPacket datagramPacket = new DatagramPacket(dataSent, dataSent.length, ipAddress, port);
				socket.send(datagramPacket);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (message.get("failure")) {
			try {
				String errorType = message.split("failure:")[1];
				boolean isCrashRequest = false;
				if (errorType.equals("byzantine")) {
					failureCount += 1;
				} else if (errorType.equals("crash")) {
					isCrashRequest = true;
				} else {
					synchronized(queueLock) {
						requestQueue.add(message);
					}
				}
				
				if (isCrashRequest || failureCount >= MAX_FAILURE_COUNT) {
					resetReplica();
				} else {
					synchronized(queueLock) {
						String currentRequest = requestQueue.poll();
						if (currentRequest != null) {
							replica.executeRequest(currentRequest);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void start() {
		this.managerThread.start();
	}
	
	public void stop() throws InterruptedException {
		this.managerThread.join();
	}
	
	public void resetReplica() {
		// TODO: get data from other replicas and pass to reset
		JSONObject currentData = new JSONObject();
		this.replica.stopServers();
		this.replica.startServers();
		this.replica.setCurrentData(currentData);
		failureCount = 0;
	}
}
