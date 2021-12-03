package Replicas.Replica3.campus;

import DRRS.Config;
import DRRS.MessageKeys;
import Replicas.CampusServerInterface;
import Replicas.Replica3.helpers.Helpers;
import Replicas.Replica3.repo.CentralRepository;
import Replicas.Replica3.utils.TextLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CampusImpl implements CampusServerInterface, Runnable {
    private static final Object createRoomRequestLock = new Object();
    private static final Object deleteRoomRequestLock = new Object();
    private static final Object bookRoomRequestLock = new Object();
    private static final Object cancelBookingRequestLock = new Object();

    private String serverName;
    private HashMap<String, HashMap<Integer, HashMap<String, RoomRecord>>> roomRecords;
    TextLogger logger;
    private int UDPPort;
    private HashMap<String, Integer> otherSocketPorts;

    public CampusImpl(String serverName, int UDPPort, HashMap<String, Integer> otherSocketPorts) {
        this.UDPPort = UDPPort;
        this.serverName = serverName;
        this.otherSocketPorts = otherSocketPorts;
        roomRecords = new HashMap<>();
        logger = new TextLogger(this.serverName + "Server_log.txt");
    }

    @Override
    public void run() {
        System.out.println("starting " + this.serverName);
        udpLoop();
    }

    private void udpLoop() {
        DatagramSocket aSocket = null;
        String msg = "";

        try {
            aSocket = new DatagramSocket(this.UDPPort);
            byte[] buffer = new byte[4096];

            while (true) {
                msg = "";
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                byte[] reqMessage = request.getData();
                // DESERIALIZATION
                ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(reqMessage, request.getOffset(), request.getLength()));
                UdpMessageInterface message = (UdpMessage) iStream.readObject();
                String opName = message.getOpName();
                // DESERIALIZATION END

                if (opName.equals("bookRoom")) {
                    int roomNumber = parseRoomNumber(message.getRoomNumber());
                    msg += this.bookRoom(message.getID(), message.getCampusName(), roomNumber, message.getDate(), message.getTimeSlot());
                } else if (opName.equals("cancelBooking")) {
                    msg += this.cancelBooking(message.getID(), message.getBookingID());
                } else if (opName.equals("getAvailableTimeSlot")) {
                    msg += this.getAvailableTimeSlot(message.getDate());
                } else if (opName.equals("changeReservation")) {
                    int roomNumber = parseRoomNumber(message.getRoomNumber());
                    msg += this.changeReservation(message.getID(), message.getBookingID(), message.getCampusName(), roomNumber, message.getTimeSlot());
                } else if (opName.equals("internalGet")) {
                    msg += this.getEmptyRoomCount(message.getDate()) + " ";
                } else if (opName.equals("createRoom")) {
                    int roomNumber = parseRoomNumber(message.getRoomNumber());
                    msg += this.createRoom(message.getID(), roomNumber, message.getDate(), message.getTimeSlots());
                } else if (opName.equals("deleteRoom")) {
                    int roomNumber = parseRoomNumber(message.getRoomNumber());
                    msg += this.deleteRoom(message.getID(), roomNumber, message.getDate(), message.getTimeSlots());
                }

                byte[] response = msg.getBytes(StandardCharsets.UTF_8);
                DatagramPacket reply = new DatagramPacket(response, response.length, request.getAddress(), request.getPort());
                aSocket.send(reply);
            }
        } catch (Exception e) {
            System.out.println("error\n" + e.getMessage());
        }
    }

    private int parseRoomNumber(String roomNumber) {
        int num = 0;
        try {
            num = Integer.parseInt(roomNumber);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Error parsing room number!");
        }
        return num;
    }

    @Override
    public synchronized String createRoom(String adminID, int roomNumber, String date, String[] timeSlots) {
        if (!authenticate(adminID)) return "Failure: failed to authenticate admin";
        String successMessage = "Success: Successfully added timeslots for " + date + " in room " + roomNumber;

        if (this.roomRecords.containsKey(date) &&
                this.roomRecords.get(date).containsKey(roomNumber)) {
            // Room number already exists, just add new timeslots
            HashMap<String, RoomRecord> previousTimeSlots = this.roomRecords.get(date).get(roomNumber);
            String rrid = "RR" + CentralRepository.getRRIDCount();
            CentralRepository.incrementRRIDCount();
//            String timeConflicts = Helpers.skipConflictingTimeSlots(previousTimeSlots, timeSlots, roomNumber, date, rrid);

            logMessage(roomNumber, "Create room", timeSlots, successMessage + "\n");

            return successMessage;
        } else if (this.roomRecords.containsKey(date)) {
            HashMap<String, RoomRecord> roomRecordHashMap = new HashMap<>();
            for (String s : timeSlots) {
                String rrid = "RR" + CentralRepository.getRRIDCount();
                CentralRepository.incrementRRIDCount();
                roomRecordHashMap.put(s, new RoomRecord(s, roomNumber, date, rrid, serverName));
            }
            this.roomRecords.get(date).put(roomNumber, roomRecordHashMap);

            logMessage(roomNumber, "Create room", timeSlots, successMessage);

            return successMessage;
        }

        HashMap<Integer, HashMap<String, RoomRecord>> roomInfo = new HashMap<>();
        HashMap<String, RoomRecord> roomRecordHashMap = new HashMap<>();
        for (String s : timeSlots) {
            String rrid = "RR" + CentralRepository.getRRIDCount();
            CentralRepository.incrementRRIDCount();
            roomRecordHashMap.put(s, new RoomRecord(s, roomNumber, date, rrid, serverName));
        }
        roomInfo.put(roomNumber, roomRecordHashMap);
        this.roomRecords.put(date, roomInfo);

        logMessage(roomNumber, "Create room", timeSlots, successMessage);


        return successMessage;
    }

    @Override
    public synchronized String deleteRoom(String adminID, int roomNumber, String date, String[] timeSlots) {
        if (!authenticate(adminID)) return "Failure: failed to authenticate admin";
        StringBuilder deletedRoomsMessage = new StringBuilder("Success: ");
        // check if room exists first
        if (this.roomRecords.containsKey(date) &&
                this.roomRecords.get(date).containsKey(roomNumber)) {
            HashMap<String, RoomRecord> previousTimeSlots = this.roomRecords.get(date).get(roomNumber);

            for (String timeSlot : timeSlots) {
                if (previousTimeSlots.remove(timeSlot) != null) {
                    deletedRoomsMessage.append("Deleted time slot: " + timeSlot + "\n");
                    ArrayList<String> bookingIDsToDelete = new ArrayList<>();
                    for (String bookingID : CentralRepository.getBookingRecord().keySet()) {
                        RoomRecord rr = CentralRepository.getBookingRecord().get(bookingID);
                        if (rr.getDate().equals(date) && rr.getRoomNumber() == roomNumber && rr.getTimeSlot().equals(timeSlot)) {
                            bookingIDsToDelete.add(bookingID);
                        }
                    }
                    for (String d : bookingIDsToDelete)
                        CentralRepository.getBookingRecord().remove(d);
                } else {
                    deletedRoomsMessage.append("Could not delete time slot: " + timeSlot + ", does not exit!\n");
                }
            }
        } else {
            logMessage(roomNumber, "Delete room", timeSlots, "Record does not exist!");
            return "Failure: Record does not exist!";
        }

        logMessage(roomNumber, "Delete room", timeSlots, deletedRoomsMessage.toString());
        return deletedRoomsMessage.toString();
    }

    private boolean authenticate(String ID) {
        return ID.charAt(3) == 'A';
    }

    @Override
    public synchronized String bookRoom(String studentID, String campusName, int roomNumber, String date, String timeSlot) {
        if (!campusName.equals(this.serverName)) {
            UdpMessage reqMessage = new UdpMessage("bookRoom", studentID, campusName, Integer.toString(roomNumber), timeSlot, date , null);
            return udpSend(reqMessage, this.otherSocketPorts.get(campusName));
        }

        RoomRecord rr = null;
        if (this.roomRecords.containsKey(date) &&
                this.roomRecords.get(date).containsKey(roomNumber)) {
            rr = this.roomRecords.get(date).get(roomNumber).get(timeSlot);

        } else {
            logMessage(roomNumber, "Book room on " + campusName, timeSlot, "Invalid selection!");
            return "Failure: Invalid selection!";
        }
        if (rr == null) {
            logMessage(roomNumber, "Book room on " + campusName, timeSlot, "Invalid selection!");
            return "Failure: Invalid selection!";
        }
        // Room is free, student can book
        if (!rr.isBooked()) {
            if (checkWeeklyBookCount(date, studentID) == 3) {
                logMessage(roomNumber, "Book room on " + campusName, timeSlot, "You can only book 3 timeslots per week!");
                return "Failure: You can only book 3 timeslots per week!";
            }
            String bookingID = rr.book(studentID);
            CentralRepository.getBookingRecord().put(bookingID, rr);

            int bookCountInWeek = checkWeeklyBookCount(date, studentID);

            logMessage(roomNumber, "Book room on " + campusName, timeSlot, bookingID + " booked on campus " + this.serverName +
                    "\nTotal rooms booked in week of " + date + ": " + bookCountInWeek);

            return "Success: " + "Room booked on campus " + this.serverName +
                    "\nTotal rooms booked in week of " + date + ": " + bookCountInWeek + " BookingID: " + bookingID ;
        } else {
            logMessage(roomNumber, "Book room on " + campusName, timeSlot, "Room already booked!");
            return "Failure: Room already booked!";
        }
    }

    @Override
    public synchronized String cancelBooking(String studentID, String bookingID) {
        RoomRecord rr = null;
//        String studentID = ID.split("-")[0];
//        String bookingID = ID.split("-")[1];
        if (CentralRepository.getBookingRecord().containsKey(bookingID)) {
            rr = CentralRepository.getBookingRecord().get(bookingID);
        } else {
            logger.log("Request Type: Cancel booking");
            logger.log("BookingID: " + bookingID);
            logger.log("Request completed");
            logger.log("Server response: No booked room for that booking ID");
            return "No booked room for that booking ID";
        }

        if (rr.getBookedBy() != null && rr.getBookedBy().equals(studentID)) {
            rr.cancelBooking();
            CentralRepository.getBookingRecord().remove(bookingID);

        } else {
            logMessage(rr.getRoomNumber(), "Cancel booking", rr.getTimeSlot(), "Can't cancel booking, room isn't booked by you!");
            return "Failure: Can't cancel booking, room isn't booked by you!";
        }

        String serverMessage ="Success: Booking canceled on " + rr.getDate() + " at " + rr.getTimeSlot();
        logMessage(rr.getRoomNumber(), "Cancel booking", rr.getTimeSlot(), serverMessage);
        String date = rr.getDate();

        return serverMessage;
    }

    @Override
    public synchronized String changeReservation(String studentID, String bookingID, String newCampusName, int newRoomNumber, String newTimeSlot) {
        if (newCampusName.equals(this.serverName)) {
            logger.log("Request Type: Change reservation");
            logger.log("BookingID: " + bookingID);
            // First check if new room number and new time slot are available, assume same date
            RoomRecord rr = null;
//            String studentID = ID.split("-")[0];
//            String bookingID = ID.split("-")[1];
            // Booking exists
            if (CentralRepository.getBookingRecord().containsKey(bookingID)) {
                rr = CentralRepository.getBookingRecord().get(bookingID);
            } else {
                logger.log("Request completed");
                logger.log("Server response: Impossible to change booking, bookingID does not exits!");
                return "Failure: Impossible to change booking, bookingID does not exits!";
            }

            // check if booking can be canceled
            if (!rr.getBookedBy().equals(studentID)) {
                logger.log("Request completed");
                logger.log("Server response: Can not alter booking, room is not booked by you!");
                return "Failure: Can not alter booking, room is not booked by you!";
            }

            // Check if new booking is available before cancelling previous one
            String previousDate = rr.getDate();
            if (this.roomRecords.containsKey(previousDate) &&
                    this.roomRecords.get(previousDate).containsKey(newRoomNumber)) {
                RoomRecord newRR = this.roomRecords.get(previousDate).get(newRoomNumber).get(newTimeSlot);
                // New room can be booked, cancel previous booking and book new room
                if (!newRR.isBooked()) {
                    this.cancelBooking(studentID, bookingID);
                    String msg = this.bookRoom(studentID, this.serverName, newRoomNumber, previousDate, newTimeSlot);
                    logger.log("Request completed");
                    logger.log("Server response: Booking changed, " + msg);
                    return "Success: Booking changed, "  + msg;
                }
            } else {
                logger.log("Request completed");
                logger.log("Server response: Impossible to change booking, new booking not available!");
                return "Failure: Impossible to change booking, new booking not available!";
            }

            return "Failure: Could not change booking, unknown error!";
        } else {
            UdpMessage reqMessage = new UdpMessage("changeBooking", studentID, newCampusName, Integer.toString(newRoomNumber), newTimeSlot, null, bookingID);
            return udpSend(reqMessage, this.otherSocketPorts.get(newCampusName));
        }
    }

    @Override
    public JSONArray getRecords() {
        JSONArray jsonRecords = new JSONArray();

        for (Map.Entry<String, HashMap<Integer, HashMap<String, RoomRecord>>> dateEntry: roomRecords.entrySet()) {
            HashMap<Integer, HashMap<String, RoomRecord>> rooms = dateEntry.getValue();
            String date = dateEntry.getKey();
            // For each room
            for (Map.Entry<Integer, HashMap<String, RoomRecord>> timeSlots : rooms.entrySet()) {
                int roomNb = timeSlots.getKey();
                HashMap<String, RoomRecord> timeSlotEntry = timeSlots.getValue();
                for (Map.Entry<String, RoomRecord> roomEntry : timeSlotEntry.entrySet()) {
                    RoomRecord rr = roomEntry.getValue();
                    JSONObject jsonRecord = new JSONObject();
                    jsonRecord.put(MessageKeys.DATE, date);
                    jsonRecord.put(MessageKeys.ROOM_NUM, roomNb);
                    jsonRecord.put(MessageKeys.TIMESLOT, rr.getTimeSlot());
                    jsonRecord.put(MessageKeys.BOOKING_ID, rr.getBookingID());
                    jsonRecord.put(MessageKeys.STUDENT_ID, rr.getBookedBy());
                    jsonRecords.add(jsonRecord);
                }
            }
        }
        return jsonRecords;
    }

    @Override
    public void setRecords(JSONArray records) {
        HashMap<String, RoomRecord> timeRoomRecord = new HashMap<>();
        HashMap<Integer, HashMap<String, RoomRecord>> roomNumberRR = new HashMap<>();

        for (Object jObj : records) {
            JSONObject record = (JSONObject) jObj;

            String date = record.get(MessageKeys.DATE).toString();
            String timeslot = record.get(MessageKeys.TIMESLOT).toString();
            String bookedBy = record.get(MessageKeys.STUDENT_ID).toString();
            String bookingId = record.get(MessageKeys.BOOKING_ID).toString();
            int roomNb = Integer.parseInt(record.get(MessageKeys.ROOM_NUM).toString());

            RoomRecord tmp = new RoomRecord(timeslot, roomNb, date, "", this.serverName);
            tmp.setBookingID(bookingId);
            tmp.setBookedBy(bookedBy);

            if (!bookedBy.equals("") && !bookingId.equals("")) {
                CentralRepository.getBookingRecord().put(bookingId, tmp);
            }
            timeRoomRecord.put(timeslot, tmp);
            roomNumberRR.put(roomNb, timeRoomRecord);

            this.roomRecords.put(date, roomNumberRR);
        }
    }

    @Override
    public synchronized String getAvailableTimeSlot(String date) {
        String msg = "";
        msg += this.getEmptyRoomCount(date);

        String[] serverList = {"DVL", "WST", "KKL"};
        for (String serverName : serverList) {
            if (!serverName.equals(this.serverName)) {
                int serverPort = this.otherSocketPorts.get(serverName);
                UdpMessage reqMessage = new UdpMessage("internalGet", null, null, null, "", date, null);
                msg += udpSend(reqMessage, serverPort) + " ";
            }
        }

        return msg;
    }

    private String udpSend(UdpMessage msg, int serverPort) {
        DatagramSocket aSocket = null;
        String res = "";
        try {
            aSocket = new DatagramSocket();

            // SERIALIZATION
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutput oo = new ObjectOutputStream(bStream);
            oo.writeObject(msg);
            oo.close();
            // SERIALIZATION END

            byte[] m = bStream.toByteArray();
            InetAddress aHost = InetAddress.getByName(Config.IPAddresses.REPLICA3);

            DatagramPacket request = new DatagramPacket(m, m.length, aHost, serverPort);
            aSocket.send(request);

            byte[] buffer = new byte[4096];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            aSocket.receive(reply);
            res += new String(reply.getData());

        } catch (SocketException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return res.trim();
    }

    private void logMessage(int roomNumber, String requestType, String[] timeSlots, String serverMessage) {
        logger.log("Request Type: " + requestType);
        logger.log("Room number: " + roomNumber);
        String ts = "";
        for (String s : timeSlots) {
            ts += s + ", ";
        }
        logger.log("Time slots: " + ts);
        logger.log("Request completed");
        logger.log("Server response: " + serverMessage );
    }

    private void logMessage(int roomNumber, String requestType, String timeSlot, String serverMessage) {
        logger.log("Request Type: " + requestType);
        logger.log("Room number: " + roomNumber);
        logger.log("Time slot: " + timeSlot);
        logger.log("Request completed");
        logger.log("Server response: " + serverMessage );
    }

    private int checkWeeklyBookCount(String date, String studentID) {

        int count = 0;
        Calendar c = Calendar.getInstance();
        c.setTime(Helpers.createDateFromString(date));
        int year1 = c.getWeekYear();
        int week1 = c.get(c.WEEK_OF_YEAR);
        Calendar c2 = Calendar.getInstance();
        for (RoomRecord rr : CentralRepository.getBookingRecord().values()) {
            if (rr.getBookedBy() != null && rr.getBookedBy().equals(studentID)) {
                String d = rr.getDate();
                c2.setTime(Helpers.createDateFromString(d));
                int year2 = c2.getWeekYear();
                int week2 = c2.get(c2.WEEK_OF_YEAR);

                if (year1 == year2 && week1 == week2)
                    count += 1;
            }
        }
        return count;
    }

    private void populateRoomRecords(String date, ArrayList<String> times, ArrayList<Integer> roomNumbers) {
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        HashMap<Integer, HashMap<String, RoomRecord>> rooms = new HashMap<>();

        for (int rn : roomNumbers) {
            HashMap<String, RoomRecord> timeSlots = new HashMap<>();
            for(String ts : times) {
                timeSlots.put(ts, new RoomRecord(ts, rn, date, "RR" + CentralRepository.getRRIDCount(), serverName));
                CentralRepository.incrementRRIDCount();
            }
            rooms.put(rn, timeSlots);
        }

        this.roomRecords.put(date, rooms);
    }

    private void init() {
        ArrayList<String> times = new ArrayList<>();
        times.add("10:00-11:00");
        times.add("11:00-12:00");
        times.add("12:00-13:00");
        ArrayList<Integer> roomNumbers = new ArrayList<>();
        roomNumbers.add(100);
        roomNumbers.add(101);
        roomNumbers.add(102);
        roomNumbers.add(103);
        ArrayList<String> dates = new ArrayList<>();
        dates.add("01-11-2021");
        dates.add("02-11-2021");
        dates.add("03-11-2021");
        dates.add("13-11-2021");
        dates.add("14-11-2021");
        dates.add("15-11-2021");
        dates.add("16-11-2021");

        for (String d : dates) {
            this.populateRoomRecords(d, times, roomNumbers);
        }
    }

    private String getEmptyRoomCount(String date) {
        int count = 0;
        if (this.roomRecords.containsKey(date)) {
            for (int roomNumber : this.roomRecords.get(date).keySet()) {
                for (String timeSlot : this.roomRecords.get(date).get(roomNumber).keySet()) {
                    if (!this.roomRecords.get(date).get(roomNumber).get(timeSlot).isBooked()) {
                        count += 1;
                    }
                }
            }
        }

        return this.serverName + " " + count + " ";
    }

}
