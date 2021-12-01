package DRRS;

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
	private final Queue<JSONObject> requestQueue;
	private final Replica replica;
	private final ReplicaPorts replicaManagerPorts;
	private final Object queueLock;
	private final Thread managerThread;
	private final Thread replicaThread;
	private int lastProcessedSequenceNumber = 0;
	private InetAddress managerAddress;
	private final JSONParser parser = new JSONParser();
	
	public ReplicaManager(int replicaNumber, Replica replica) throws UnknownHostException {
		this.replicaManagerPorts = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(replicaNumber);
		managerAddress = InetAddress.getByName(replicaManagerPorts.getRmIpAddress());
		
		this.replica = replica;
		managerThread = new Thread(new ManagerThread());
		replicaThread = new Thread(new ReplicaThread());
		this.requestQueue = new LinkedBlockingQueue<>();
		this.queueLock = new Object();
	}
	
	class ManagerThread implements Runnable {
		@Override
		public void run() {
			try (DatagramSocket socket = new DatagramSocket(replicaManagerPorts.getRmPort())) {
				socket.setSoTimeout(1000);
				while(true) {
					byte[] receivedBytes = new byte[1024];
					DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, receivedBytes.length);
					socket.receive(receivedPacket);
					process(socket, receivedPacket);
				}
			} catch(IOException | ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	class ReplicaThread implements Runnable {
		@Override
		public void run() {
			replica.startServers();
			while (true) {
				synchronized(queueLock) {
					JSONObject currentRequest = requestQueue.poll();
					if (currentRequest != null) {
						int sequenceNumber = Integer.parseInt(currentRequest.get(MessageKeys.SEQ_NUM).toString());
						
						while (sequenceNumber <= lastProcessedSequenceNumber) {
							currentRequest = requestQueue.remove();
							sequenceNumber = Integer.parseInt(currentRequest.get(MessageKeys.SEQ_NUM).toString());
							
							System.out.println("\nSkipping already processed request" + sequenceNumber);
						}
						
						lastProcessedSequenceNumber = sequenceNumber;
						
						String message = replica.executeRequest(currentRequest);
						Config.StatusCode statusCode = Config.StatusCode.SUCCESS;

						if (message.contains("INVALID") || message.contains("Failure") ||
								message.contains("Error")) {
							statusCode = Config.StatusCode.FAIL;
						}
						
						JSONObject returnObject = new JSONObject();
						returnObject.put(MessageKeys.RM_PORT_NUMBER, replicaManagerPorts.getRmPort());
						returnObject.put(MessageKeys.STATUS_CODE, statusCode.toString());
						returnObject.put(MessageKeys.MESSAGE, message);
						
						try (DatagramSocket socket = new DatagramSocket()) {
							byte[] dataSent = returnObject.toJSONString().getBytes();
							socket.send(new DatagramPacket(dataSent, dataSent.length,  InetAddress.getByName(Config.IPAddresses.FRONT_END), Config.PortNumbers.RE_FE));
						} catch(IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	public void process(DatagramSocket socket, DatagramPacket request) throws ParseException {
		JSONObject requestData = (JSONObject) parser.parse(new String(request.getData()).trim());
		String command = requestData.get(MessageKeys.COMMAND_TYPE).toString();
		
		if (command.equals(Config.GET_DATA)) {
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
		} else if (command.equals(Config.REPORT_FAILURE)) {
			try {
				String errorType = requestData.get(MessageKeys.FAILURE_TYPE).toString();
				boolean isCrashRequest = false;
				if (errorType.equals(Config.Failure.BYZANTINE.toString())) {
					failureCount += 1;
				} else if (errorType.equals(Config.Failure.PROCESS_CRASH.toString())) {
					isCrashRequest = true;
				}
				
				if (isCrashRequest || failureCount >= MAX_FAILURE_COUNT) {
					synchronized(queueLock) {
						resetReplica();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			synchronized(queueLock) {
				requestQueue.add(requestData);
			}
		}
	}
	
	public void start() {
		this.managerThread.start();
		this.replicaThread.start();
	}
	
	public void stop() throws InterruptedException {
		this.managerThread.join();
		this.replicaThread.join();
		this.replica.stopServers();
	}
	
	public void resetReplica() throws InterruptedException {
		// TODO: get data from other replicas and pass to reset
		JSONObject currentData = new JSONObject();
		this.replica.stopServers();
		this.replica.startServers();
		this.replica.setCurrentData(currentData);
		failureCount = 0;
	}
}
