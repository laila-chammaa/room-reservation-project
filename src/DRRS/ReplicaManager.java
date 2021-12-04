package DRRS;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReplicaManager {
	private static final int MAX_FAILURE_COUNT = 3;
	private int failureCount;
	private final Queue<JSONObject> requestQueue;
	private final Replica replica;
	private final int replicaNumber;
	private final ReplicaPorts replicaManagerPorts;
	private final Object queueLock;
	private final Thread managerThread;
	private Thread replicaThread;
	private final Thread queueThread;
	private int lastProcessedSequenceNumber = 0;
	private final InetAddress managerAddress;
	private final JSONObject getDataObject;
	private final JSONParser parser = new JSONParser();
	private final Random randomizer = new Random();
	private AtomicBoolean keepProcessing = new AtomicBoolean(false);
	
	public ReplicaManager(int replicaNumber, Replica replica) throws UnknownHostException {
		this.replicaNumber = replicaNumber;
		this.replicaManagerPorts = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(replicaNumber);
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
		
		private void sendAcknowledgement(int sequenceNumber) {
			JSONObject jsonAck = new JSONObject();
			jsonAck.put(MessageKeys.SEQ_NUM, sequenceNumber);
			jsonAck.put(MessageKeys.RM_PORT_NUMBER, replicaManagerPorts.getRmPort());
			jsonAck.put(MessageKeys.COMMAND_TYPE, Config.ACK);
			byte[] ack = jsonAck.toString().getBytes();
			try (DatagramSocket socket = new DatagramSocket()) {
				DatagramPacket packet = new DatagramPacket(ack, ack.length, InetAddress.getByName(Config.IPAddresses.SEQUENCER),
						Config.PortNumbers.FE_SEQ);
				socket.send(packet);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			try (MulticastSocket socket = new MulticastSocket(Config.PortNumbers.SEQ_RE)) {
				socket.joinGroup(InetAddress.getByName(Config.IPAddresses.MULTICAST_ADR));
				while(true) {
					byte[] receivedBytes = new byte[1024];
					DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, receivedBytes.length);
					socket.receive(receivedPacket);
					JSONObject requestData = (JSONObject) parser.parse(new String(receivedPacket.getData()).trim());
					sendAcknowledgement(Integer.parseInt(requestData.get(MessageKeys.SEQ_NUM).toString()));
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
			while (true) {
				synchronized(queueLock) {
					JSONObject currentRequest = requestQueue.poll();
					if (currentRequest != null) {
						int sequenceNumber = Integer.parseInt(currentRequest.get(MessageKeys.SEQ_NUM).toString());
						
						while (sequenceNumber <= lastProcessedSequenceNumber) {
							currentRequest = requestQueue.remove();
							sequenceNumber = Integer.parseInt(currentRequest.get(MessageKeys.SEQ_NUM).toString());
							
							System.out.println("\nSkipping already processed request " + sequenceNumber);
						}
						
						lastProcessedSequenceNumber = sequenceNumber;
						
						String messageId = currentRequest.get(MessageKeys.MESSAGE_ID).toString();
						String message = replica.executeRequest(currentRequest);
						Config.StatusCode statusCode = Config.StatusCode.SUCCESS;

						if (message.toLowerCase().contains("invalid") || message.toLowerCase().contains("failure") ||
								message.toLowerCase().contains("error")) {
							statusCode = Config.StatusCode.FAIL;
						}
						
						JSONObject returnObject = new JSONObject();
						returnObject.put(MessageKeys.RM_PORT_NUMBER, replicaManagerPorts.getRmPort());
						returnObject.put(MessageKeys.STATUS_CODE, statusCode.toString());
						returnObject.put(MessageKeys.MESSAGE, message);
						returnObject.put(MessageKeys.MESSAGE_ID, messageId);
						
						try (DatagramSocket socket = new DatagramSocket()) {
							byte[] dataSent = returnObject.toJSONString().getBytes();
							socket.send(new DatagramPacket(dataSent, dataSent.length, InetAddress.getByName(Config.IPAddresses.FRONT_END), Config.PortNumbers.RE_FE));
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
					System.out.println("Replica " + replicaNumber + ": incrementing failure count.");
					failureCount += 1;
				} else if (errorType.equals(Config.Failure.PROCESS_CRASH.toString())) {
					System.out.println("Replica " + replicaNumber + ": process crash detected.");
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
		replica.startServers();
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
			
			DatagramPacket packet = new DatagramPacket(
					getDataBytes, getDataBytes.length, InetAddress.getByName(otherPorts.getRmIpAddress()), otherPorts.getRmPort()
			);
			socket.send(packet);
			
			byte[] receiveDataBytes = new byte[256000];
			DatagramPacket receivedPacket = new DatagramPacket(receiveDataBytes, receiveDataBytes.length);
			socket.receive(receivedPacket);
			
			return (JSONObject) parser.parse(new String(receivedPacket.getData()).trim());
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void resetReplica() throws InterruptedException {
		System.out.println("Replica " + replicaNumber + ": restarting replica.");
		
		this.replicaThread.interrupt();
		
		JSONObject currentData = null;
		
		try(DatagramSocket socket = new DatagramSocket(9999, managerAddress)) {
			int pickedReplicaNb;
			while(currentData == null) {
				// Get valid random replica
				while ((pickedReplicaNb = randomizer.nextInt(4) + 1) == replicaNumber);
				currentData = requestGetData(socket, Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(pickedReplicaNb));
				System.out.println("Replica " + replicaNumber + ": retrieved data from replica" + pickedReplicaNb + ".");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		this.replica.setCurrentData(currentData);
		this.replicaThread = new Thread(new ReplicaThread());
		this.replicaThread.start();
		
		System.out.println("Replica " + replicaNumber + ": restart complete.");
		failureCount = 0;
	}
}
