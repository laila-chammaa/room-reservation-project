package Frontend;

import DRRS.Config;
import DRRS.MessageKeys;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.jws.WebService;
import java.io.IOException;
import java.net.*;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@WebService(endpointInterface = "Frontend.FrontendInterface")
public class FrontendImpl implements FrontendInterface {

    private Semaphore receiveFromReplica = new Semaphore(0);
    private ConcurrentHashMap<Integer, Message> messages = new ConcurrentHashMap<>();
    private long longestTimeout = 5000;
    private JSONParser parser = new JSONParser();
    private int messageID = 1;
    private final AtomicBoolean listeningForResponses = new AtomicBoolean(true);

    private static final int USER_TYPE_POS = 3;

    private Logger logger;

    public FrontendImpl() {

        ReplicaResponseListener responseListener = new ReplicaResponseListener();
        Thread responseListenerThread = new Thread(responseListener);

        responseListenerThread.start();

        initiateLogger();
        this.logger.info("Initializing Frontend ...");
        this.logger.info("Server: Frontend initializing success.");
    }

    private void initiateLogger() {
        Logger logger = Logger.getLogger("Server Logs/Frontend Log");
        FileHandler fh;

        try {
            //FileHandler Configuration and Format Configuration
            fh = new FileHandler("Server Logs/Frontend Log.log");

            //Disable console handling
            //logger.setUseParentHandlers(false);
            logger.addHandler(fh);

            //Formatting configuration
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException e) {
            System.err.println("Server Log: Error: Security Exception " + e);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Server Log: Error: IO Exception " + e);
            e.printStackTrace();
        }

        System.out.println("Server Log: Logger initialization success.");

        this.logger = logger;
    }

    @Override
    public String createRoom(String adminID, int roomNumber, String date, String[] listOfTimeSlots) {
        JSONObject payload = new JSONObject();
        Message message = null;

        payload.put(MessageKeys.COMMAND_TYPE, Config.CREATE_ROOM);
        payload.put(MessageKeys.ADMIN_ID, adminID);
        payload.put(MessageKeys.ROOM_NUM, roomNumber);
        payload.put(MessageKeys.DATE, date);
        payload.put(MessageKeys.TIMESLOTS, listOfTimeSlots);

        try {
            message = createMessage(payload);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return makeRequest(message);
    }

    @Override
    public String deleteRoom(String adminID, int roomNumber, String date, String[] listOfTimeSlots) {

        JSONObject payload = new JSONObject();
        Message message = null;

        payload.put(MessageKeys.COMMAND_TYPE, Config.DELETE_ROOM);
        payload.put(MessageKeys.ADMIN_ID, adminID);
        payload.put(MessageKeys.ROOM_NUM, roomNumber);
        payload.put(MessageKeys.DATE, date);
        payload.put(MessageKeys.TIMESLOTS, listOfTimeSlots);

        try {
            message = createMessage(payload);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return makeRequest(message);
    }

    @Override
    public String bookRoom(String studentID, String campusID, int roomNumber, String date,
                           String timeslot) {

        JSONObject payload = new JSONObject();
        Message message = null;

        payload.put(MessageKeys.COMMAND_TYPE, Config.BOOK_ROOM);
        payload.put(MessageKeys.STUDENT_ID, studentID);
        payload.put(MessageKeys.CAMPUS, campusID);
        payload.put(MessageKeys.ROOM_NUM, roomNumber);
        payload.put(MessageKeys.DATE, date);
        payload.put(MessageKeys.TIMESLOT, timeslot);

        try {
            message = createMessage(payload);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return makeRequest(message);
    }

    @Override
    public String cancelBooking(String studentID, String bookingID) {
        JSONObject payload = new JSONObject();
        Message message = null;

        payload.put(MessageKeys.COMMAND_TYPE, Config.CANCEL_BOOKING);
        payload.put(MessageKeys.STUDENT_ID, studentID);
        payload.put(MessageKeys.BOOKING_ID, bookingID);

        try {
            message = createMessage(payload);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return makeRequest(message);
    }

    @Override
    public synchronized String changeReservation(String studentID, String bookingId, String newCampusName, int newRoomNo,
                                                 String newTimeSlot) {
        this.logger.info(String.format("Server Log | Request: changeReservation | StudentID: %s | " +
                        "BookingID: %s | New CampusID: %s | New room: %d | New Timeslot: %s", studentID, bookingId,
                newCampusName, newRoomNo, newTimeSlot));

        JSONObject payload = new JSONObject();
        Message message = null;

        payload.put(MessageKeys.COMMAND_TYPE, Config.CHANGE_RESERVATION);
        payload.put(MessageKeys.STUDENT_ID, studentID);
        payload.put(MessageKeys.BOOKING_ID, bookingId);
        payload.put(MessageKeys.CAMPUS, newCampusName);
        payload.put(MessageKeys.ROOM_NUM, newRoomNo);
        payload.put(MessageKeys.TIMESLOT, newTimeSlot);

        try {
            message = createMessage(payload);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return makeRequest(message);
    }

    @Override
    public String getAvailableTimeSlot(String studentId, String date) {
        this.logger.info(String.format("Server Log | Request: getAvailableTimeSlot | Date: %s", date));

        JSONObject payload = new JSONObject();
        Message message = null;

        payload.put(MessageKeys.COMMAND_TYPE, Config.GET_TIMESLOTS);
        payload.put(MessageKeys.STUDENT_ID, studentId);
        payload.put(MessageKeys.DATE, date);

        try {
            message = createMessage(payload);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return makeRequest(message);
    }

    private String validateAdmin(String userID) {
        char userType = userID.charAt(USER_TYPE_POS);
        if (userType != 'A') {
            return "Login Error: This request is for admins only.";
        }
        return null;
    }

    private String validateStudent(String userID) {
        char userType = userID.charAt(USER_TYPE_POS);
        if (userType != 'S') {
            return "Login Error: This request is for students only.";
        }
        return null;
    }

    private class SendToSequencer implements Callable<String> {
        Message message;

        public SendToSequencer(Message message) {
            this.message = message;
        }

        public String call() {
            DatagramSocket senderSocket = null;
            DatagramSocket receiverSocket = null;

            try {
                senderSocket = new DatagramSocket();
                receiverSocket = new DatagramSocket(Config.PortNumbers.SEQ_FE);

                byte[] messageBuffer = message.getSendData().toString().getBytes();
                InetAddress host = InetAddress.getByName(Config.IPAddresses.SEQUENCER);
                DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, Config.PortNumbers.FE_SEQ);

                System.out.println("Sending message to sequencer: " + message.getSendData().toJSONString());
                senderSocket.send(request);

                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                receiverSocket.receive(reply);
                String replyString = new String(reply.getData(), reply.getOffset(), reply.getLength());
                JSONObject jsonMessage = (JSONObject) parser.parse(replyString);

                if (jsonMessage.get(MessageKeys.COMMAND_TYPE).toString().equals(Config.ACK)) {
                    System.out.println("Message " + message.getId() + " was successfully received by the Sequencer!");
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Server on port " + Config.PortNumbers.FE_SEQ + " is not responding.");
            } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            } finally {
                if (senderSocket != null) {
                    senderSocket.close();
                }
                if (receiverSocket != null) {
                    receiverSocket.close();
                }
            }

            startProcessCrashMonitor();
            return waitForResponse();
        }

        private String waitForResponse() {
            String response = null;
            Callable<String> waitForResponseCall = new ProcessReplicaResponse(message);
            FutureTask<String> waitForResponseTask = new FutureTask<>(waitForResponseCall);
            Thread waitForResponseThread = new Thread(waitForResponseTask);

            waitForResponseThread.start();

            try {
                response = waitForResponseTask.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            return response;
        }

        private void startProcessCrashMonitor() {
            ProcessCrashMonitor processCrashMonitor = new ProcessCrashMonitor(message);
            Thread processCrashMonitorThread = new Thread(processCrashMonitor);
            processCrashMonitorThread.start();
        }
    }

    private class ProcessReplicaResponse implements Callable<String> {
        Message message;

        public ProcessReplicaResponse(Message message) {
            this.message = message;
            System.out.println("Waiting for responses for message ID: " + message.getId());
        }

        public String call() {
            String response;
            ReturnMessage message1, message2, message3, message4;

            while (true) {
                try {
                    receiveFromReplica.acquire(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (message.getReturnMessages().isEmpty()) {
                    receiveFromReplica.release(2);
                    continue;
                }

                //TODO: if we get 2 correct responses, just return? or always wait for 4?
                if (message.getReturnMessages().size() == 2) {
                    message1 = message.getReturnMessages().get(0);
                    message2 = message.getReturnMessages().get(1);

                    if (message1.code.equals(message2.code)) {
                        response = message1.message;
                        break;
                    }
                    receiveFromReplica.release(2);
                } else if (message.getReturnMessages().size() == 4) {
                    message1 = message.getReturnMessages().get(0);
                    message2 = message.getReturnMessages().get(1);

                    Optional<ReturnMessage> incorrectMessage = findIncorrectMessage(message);

                    if (incorrectMessage.isPresent()) {
                        int port = incorrectMessage.get().port;
                        notifyReplicaOfByzantineFailure(port, getIPFromPort(port));
                        //return correct message to client
                        if (!message1.equals(incorrectMessage.get())) {
                            response = message1.message; // Response to client.
                        } else {
                            response = message2.message; // Response to client.
                        }
                    } else {
                        response = message1.message; // Response to client.
                    }
                    break;
                } else {
                    receiveFromReplica.release(2);
                }
            }

            return response;
        }

        private Optional<ReturnMessage> findIncorrectMessage(Message message) {
            ReturnMessage message1, message2, message3, message4;

            message1 = message.getReturnMessages().get(0);
            message2 = message.getReturnMessages().get(1);
            message3 = message.getReturnMessages().get(2);
            message4 = message.getReturnMessages().get(3);

            if (message1.code.equals(message2.code) && message2.code.equals(message3.code)
                    && message3.code.equals(message4.code)) {
                return Optional.empty();
            } else if (message1.code.equals(message2.code) && message2.code.equals(message3.code)) {
                //message 4 is different
                return Optional.of(message4);
            } else if (message1.code.equals(message2.code) && message2.code.equals(message4.code)) {
                //message 3 is different
                return Optional.of(message3);
            } else if (message1.code.equals(message3.code) && message3.code.equals(message4.code)) {
                //message 2 is different
                return Optional.of(message2);
            } else if (message2.code.equals(message3.code) && message3.code.equals(message4.code)) {
                //message 1 is different
                return Optional.of(message1);
            } else {
                //more than one message is incorrect, no way to find the correct one
                //TODO: throw error?
                return Optional.empty();
            }
        }

        private String getIPFromPort(int port) {
            
            switch (port) {
                case Config.Ports.REPLICA_PORT_1:
                    return Config.IPAddresses.REPLICA1;
                case Config.Ports.REPLICA_PORT_2:
                    return Config.IPAddresses.REPLICA2;
                case Config.Ports.REPLICA_PORT_3:
                    return Config.IPAddresses.REPLICA3;
                case Config.Ports.REPLICA_PORT_4:
                    return Config.IPAddresses.REPLICA4;
            }

            return null;
        }
    }


    private class ReplicaResponseListener extends Thread {
        @Override
        public void run() {
            System.out.println("Listening for responses from the replicas on port " + Config.PortNumbers.RE_FE + "...");

            DatagramSocket datagramSocket = null;

            try {
                datagramSocket = new DatagramSocket(Config.PortNumbers.RE_FE);
                datagramSocket.setSoTimeout(1000);

                while (listeningForResponses.get()) {
                    try {
                        byte[] buffer = new byte[1000];
                        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

                        datagramSocket.receive(responsePacket);
                        String data = new String(responsePacket.getData()).trim();
                        System.out.println(data);
                        JSONObject jsonMessage = (JSONObject) parser.parse(data);

                        String commandType = (String) jsonMessage.get(MessageKeys.COMMAND_TYPE);
                        if (commandType != null && commandType.equals(Config.ACK)) {
                            System.out.println("received ack from RM " + responsePacket.getPort() + ". jsonMessage: " + jsonMessage.toJSONString());
                            continue;
                        }

                        int port = Integer.parseInt(jsonMessage.get(MessageKeys.RM_PORT_NUMBER).toString());
                        Message message = messages.get(Integer.parseInt(jsonMessage.get(MessageKeys.MESSAGE_ID).toString()));
                        ReturnMessage returnMessage = new ReturnMessage(port, jsonMessage.get(MessageKeys.MESSAGE).toString(), jsonMessage.get(MessageKeys.STATUS_CODE).toString());
                        message.setReturnMessage(returnMessage);

                        clockTime(message, port);

                        receiveFromReplica.release();
                        System.out.println("Received response from Replica on port " + port + " for message ID: " + jsonMessage.get(MessageKeys.MESSAGE_ID).toString() + " Semaphore: " + receiveFromReplica.availablePermits());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (IOException e) {

                    }
                }
            } catch (SocketException e1) {
                e1.printStackTrace();
            } finally {
                if (datagramSocket != null) {
                    datagramSocket.close();
                }

                System.out.println("Not listening for responses any longer.");
            }
        }
    }

    private class ProcessCrashMonitor implements Runnable {
        Message message;
        long startTime;
        long elapsedTime = 0;

        public ProcessCrashMonitor(Message message) {
            this.message = message;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            while (elapsedTime < 2 * longestTimeout) {
                if (message.getReturnMessages().size() == 4) {
                    System.out.println("Got 4 messages!");
                    break;
                }

                elapsedTime = System.currentTimeMillis() - startTime;
            }

            System.out.println("Keys: " + message.getReturnTimes().keySet().toString());

            if (!message.getReturnTimes().containsKey(Config.Ports.REPLICA_PORT_1)) {
                notifyReplicaOfProcessCrash(Config.Ports.REPLICA_PORT_1);
            }

            if (!message.getReturnTimes().containsKey(Config.Ports.REPLICA_PORT_2)) {
                notifyReplicaOfProcessCrash(Config.Ports.REPLICA_PORT_2);
            }

            if (!message.getReturnTimes().containsKey(Config.Ports.REPLICA_PORT_3)) {
                notifyReplicaOfProcessCrash(Config.Ports.REPLICA_PORT_3);
            }

            if (!message.getReturnTimes().containsKey(Config.Ports.REPLICA_PORT_4)) {
                notifyReplicaOfProcessCrash(Config.Ports.REPLICA_PORT_4);
            }
        }
    }

    private void notifyReplicaOfByzantineFailure(int port, String ipAddress) {
        DatagramSocket socket = null;
        JSONObject payload = new JSONObject();

        payload.put(MessageKeys.COMMAND_TYPE, Config.REPORT_FAILURE);
        payload.put(MessageKeys.FAILURE_TYPE, Config.Failure.BYZANTINE.toString());
        payload.put(MessageKeys.RM_PORT_NUMBER, port);

        try {
            socket = new DatagramSocket();
            byte[] messageBuffer = payload.toString().getBytes();
            InetAddress host = InetAddress.getByName(ipAddress);
            DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, port);

            System.out.println("Sending byzantine error message to Replica Manager " + port);
            socket.send(request);
        } catch (SocketTimeoutException e) {
            System.out.println("Replica Manager on port " + port + " is not responding.");
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private void notifyReplicaOfProcessCrash(int port) {
        DatagramSocket socket = null;
        JSONObject payload = new JSONObject();
        int[] ports = new int[]{Config.Ports.REPLICA_PORT_1, Config.Ports.REPLICA_PORT_2, Config.Ports.REPLICA_PORT_3};
        String[] hosts = new String[]{Config.IPAddresses.REPLICA1, Config.IPAddresses.REPLICA2, Config.IPAddresses.REPLICA3};

        payload.put(MessageKeys.COMMAND_TYPE, Config.REPORT_FAILURE);
        payload.put(MessageKeys.FAILURE_TYPE, Config.Failure.PROCESS_CRASH.toString());
        payload.put(MessageKeys.RM_PORT_NUMBER, port);

        try {
            socket = new DatagramSocket();
            byte[] messageBuffer = payload.toString().getBytes();

            for (int i = 0; i < 3; i++) {
                InetAddress host = InetAddress.getByName(hosts[i]);
                DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, ports[i]);
                socket.send(request);
                System.out.println("Sending process crash error message to Replica Manager " + ports[i] + " about Replica on port: " + port);
            }

        } catch (SocketTimeoutException e) {
            System.out.println("Replica Manager on port " + port + " is not responding.");
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private String makeRequest(Message message) {
        String response = null;
        Callable<String> sendToSequencerCall = new SendToSequencer(message);
        FutureTask<String> sendToSequencerTask = new FutureTask<>(sendToSequencerCall);
        Thread sendToSequencerThread = new Thread(sendToSequencerTask);

        startTimer(message);
        sendToSequencerThread.start();

        try {
            response = sendToSequencerTask.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return response;
    }

    private void clockTime(Message message, int port) {
        long currentTime = System.currentTimeMillis();
        long startTime = message.getStartTime();
        long difference = currentTime - startTime;

        message.setReturnTime(port, difference);

        if (difference > longestTimeout) {
            longestTimeout = difference;
        }
    }

    private void startTimer(Message message) {
        message.setStartTime(System.currentTimeMillis());
    }

    private synchronized Message createMessage(JSONObject payload) throws InterruptedException {
        payload.put(MessageKeys.MESSAGE_ID, messageID);
        Message message = new Message(messageID);
        message.setSendData(payload);
        messages.put(messageID, message);
        messageID++;

        return message;
    }

    @Override
    public void shutdown() {
        listeningForResponses.set(false);
    }
}
