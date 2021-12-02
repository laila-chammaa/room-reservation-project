package DRRS;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

public class ReplicaManager {
	private static final int MAX_FAILURE_COUNT = 3;
	private int failureCount;
	private final Queue<JSONObject> requestQueue;
	private final Replica replica;
	private final ReplicaPorts replicaManagerPorts;
	private ReplicaPorts otherPorts1 = null;
	private ReplicaPorts otherPorts2 = null;
	private ReplicaPorts otherPorts3 = null;
	private final Object queueLock;
	private final Thread managerThread;
	private final Thread replicaThread;
	private final Thread queueThread;
	private int lastProcessedSequenceNumber = 0;
	private final InetAddress managerAddress;
	private final JSONObject getDataObject;
	private final JSONParser parser = new JSONParser();
	private final Random randomizer = new Random();
	
	public ReplicaManager(int replicaNumber, Replica replica) throws UnknownHostException {
		this.replicaManagerPorts = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(replicaNumber);
		for (int i = 1; i <= 4; i++) {
			if (otherPorts1 == null && i != replicaNumber) {
				otherPorts1 = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(i);
			} else if (otherPorts2 == null && i != replicaNumber) {
				otherPorts2 = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(i);
			} else if (otherPorts3 == null && i != replicaNumber) {
				otherPorts3 = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(i);
			}
		}
		managerAddress = InetAddress.getByName(replicaManagerPorts.getRmIpAddress());
		
		getDataObject = new JSONObject();
		getDataObject.put(MessageKeys.COMMAND_TYPE, Config.GET_DATA);
		
		this.replica = replica;
		managerThread = new Thread(new ManagerThread());
		replicaThread = new Thread(new ReplicaThread());
		queueThread = new Thread(new QueueThread());
		this.requestQueue = new LinkedBlockingQueue<>();
		this.queueLock = new Object();
	}
	
	class ManagerThread implements Runnable {
		@Override
		public void run() {
			try (DatagramSocket socket = new DatagramSocket(replicaManagerPorts.getRmPort(), managerAddress)) {
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
	
	class QueueThread implements Runnable {
		@Override
		public void run() {
			try (MulticastSocket socket = new MulticastSocket(Config.PortNumbers.SEQ_RE)) {
				socket.joinGroup(InetAddress.getByName(Config.IPAddresses.MULTICAST));
				while(true) {
					byte[] receivedBytes = new byte[1024];
					DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, receivedBytes.length);
					socket.receive(receivedPacket);
					JSONObject requestData = (JSONObject) parser.parse(new String(receivedPacket.getData()).trim());
					synchronized(queueLock) {
						requestQueue.add(requestData);
					}
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
		}
	}
	
	public void start() {
		this.queueThread.start();
		this.managerThread.start();
		this.replicaThread.start();
	}
	
	public void stop() throws InterruptedException {
		this.replicaThread.join();
		this.managerThread.join();
		this.queueThread.join();
		this.replica.stopServers();
	}
	
	private JSONObject requestGetData(DatagramSocket socket, ReplicaPorts otherPorts) {
		try {
			byte[] getDataBytes = getDataObject.toString().getBytes();
			
			DatagramPacket datagramPacket1 = new DatagramPacket(
					getDataBytes, getDataBytes.length, InetAddress.getByName(otherPorts.getRmIpAddress()), otherPorts.getRmPort()
			);
			socket.send(datagramPacket1);
			
			byte[] receiveDataBytes = new byte[1024];
			DatagramPacket receivedPacket = new DatagramPacket(receiveDataBytes, receiveDataBytes.length);
			socket.receive(receivedPacket);
			
			return (JSONObject) parser.parse(new String(receivedPacket.getData()).trim());
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void resetReplica() throws InterruptedException {
		JSONObject currentData = null;
		
		try(DatagramSocket socket = new DatagramSocket()) {
			socket.setSoTimeout(1000);
			ArrayList<JSONObject> dataObjects = new ArrayList<>();
			JSONObject dataObject1 = requestGetData(socket, otherPorts1);
			JSONObject dataObject2 = requestGetData(socket, otherPorts2);
			JSONObject dataObject3 = requestGetData(socket, otherPorts3);
			
			if (dataObject1 != null) {
				dataObjects.add(dataObject1);
			}
			if (dataObject2 != null) {
				dataObjects.add(dataObject2);
			}
			if (dataObject3 != null) {
				dataObjects.add(dataObject3);
			}
			
			int objectToPick = randomizer.nextInt(dataObjects.size());
			currentData = dataObjects.get(objectToPick);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		this.replica.stopServers();
		this.replica.startServers();
		this.replica.setCurrentData(currentData);
		failureCount = 0;
	}
}
