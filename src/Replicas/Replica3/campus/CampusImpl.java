package Replicas.Replica3.campus;

import Replicas.Replica3.helpers.Helpers;
import Replicas.Replica3.repo.CentralRepository;
import Replicas.Replica3.utils.TextLogger;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class CampusImpl implements CampusInterface {
    private String serverName;
    private HashMap<String, HashMap<Integer, HashMap<String, RoomRecord>>> roomRecords;
    TextLogger logger;

    public CampusImpl(String serverName) {
        this.serverName = serverName;
        roomRecords = new HashMap<>();
        logger = new TextLogger(this.serverName + "Server_log.txt");
        new Thread(() -> udpLoop()).start();

        init();
    }

    private void udpLoop() {
        DatagramSocket aSocket = null;
        String msg = "";

        try {
            aSocket = new DatagramSocket(CentralRepository.getUdpPortNum(this.serverName));
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
                    msg += this.cancelBooking(message.getBookingID());
                } else if (opName.equals("getAvailableTimeSlot")) {
                    msg += this.getAvailableTimeSlot(message.getDate());
                } else if (opName.equals("changeReservation")) {
                    int roomNumber = parseRoomNumber(message.getRoomNumber());
                    msg += this.changeReservation(message.getID(), message.getCampusName(), roomNumber, message.getTimeSlot());
                } else if (opName.equals("internalGet")) {
                    msg += this.getEmptyRoomCount(message.getDate()) + " ";
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
    public synchronized String createRoom(int roomNumber, String date, String[] timeSlots) {
        String successMessage = "Successfully added timeslots for " + date + " in room " + roomNumber;

        if (this.roomRecords.containsKey(date) &&
                this.roomRecords.get(date).containsKey(roomNumber)) {
            // Room number already exists, just add new timeslots
            HashMap<String, RoomRecord> previousTimeSlots = this.roomRecords.get(date).get(roomNumber);
            String rrid = "RR" + CentralRepository.getRRIDCount();
            CentralRepository.incrementRRIDCount();
            String timeConflicts = Helpers.skipConflictingTimeSlots(previousTimeSlots, timeSlots, roomNumber, date, rrid);

            logMessage(roomNumber, "Create room", timeSlots, successMessage + "\n" + timeConflicts);

            return successMessage + "\n" + timeConflicts;
        } else if (this.roomRecords.containsKey(date)) {
            HashMap<String, RoomRecord> roomRecordHashMap = new HashMap<>();
            for (String s : timeSlots) {
                String rrid = "RR" + CentralRepository.getRRIDCount();
                CentralRepository.incrementRRIDCount();
                roomRecordHashMap.put(s, new RoomRecord(s, roomNumber, date, rrid));
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
            roomRecordHashMap.put(s, new RoomRecord(s, roomNumber, date, rrid));
        }
        roomInfo.put(roomNumber, roomRecordHashMap);
        this.roomRecords.put(date, roomInfo);

        logMessage(roomNumber, "Create room", timeSlots, successMessage);


        return successMessage;
    }

    @Override
    public synchronized String deleteRoom(int roomNumber, String date, String[] timeSlots) {
        StringBuilder deletedRoomsMessage = new StringBuilder();
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
            return "Record does not exist!";
        }

        logMessage(roomNumber, "Delete room", timeSlots, deletedRoomsMessage.toString());

        return deletedRoomsMessage.toString();
    }

    @Override
    public synchronized String bookRoom(String studentID, String campusName, int roomNumber, String date, String timeSlot) {
        if (!campusName.equals(this.serverName)) {
            UdpMessage reqMessage = new UdpMessage("bookRoom", studentID, campusName, Integer.toString(roomNumber), timeSlot, date , null);
            return udpSend(reqMessage, CentralRepository.getUdpPortNum(campusName));
        }

        RoomRecord rr = null;
        if (this.roomRecords.containsKey(date) &&
                this.roomRecords.get(date).containsKey(roomNumber)) {
            rr = this.roomRecords.get(date).get(roomNumber).get(timeSlot);

        } else {
            logMessage(roomNumber, "Book room on " + campusName, timeSlot, "Invalid selection!");
            return "Invalid selection!";
        }
        if (rr == null) {
            logMessage(roomNumber, "Book room on " + campusName, timeSlot, "Invalid selection!");
            return "Invalid selection!";
        }
        // Room is free, student can book
        if (!rr.isBooked()) {
            if (checkWeeklyBookCount(date, studentID) == 3) {
                logMessage(roomNumber, "Book room on " + campusName, timeSlot, "You can only book 3 timeslots per week!");
                return "You can only book 3 timeslots per week!";
            }
            String bookingID = rr.book(studentID);
            CentralRepository.getBookingRecord().put(bookingID, rr);

            int bookCountInWeek = checkWeeklyBookCount(date, studentID);

            logMessage(roomNumber, "Book room on " + campusName, timeSlot, bookingID + " booked on campus " + this.serverName +
                    "\nTotal rooms booked in week of " + date + ": " + bookCountInWeek);

            return bookingID + " booked on campus " + this.serverName +
                    "\nTotal rooms booked in week of " + date + ": " + bookCountInWeek;
        } else {
            logMessage(roomNumber, "Book room on " + campusName, timeSlot, "Room already booked!");
            return "Room already booked!";
        }
    }

    @Override
    public synchronized String cancelBooking(String ID) {
        RoomRecord rr = null;
        String studentID = ID.split("-")[0];
        String bookingID = ID.split("-")[1];
        if (CentralRepository.getBookingRecord().containsKey(bookingID)) {
            rr = CentralRepository.getBookingRecord().get(bookingID);
        } else {
            logger.log("Request Type: Cancel booking");
            logger.log("BookingID: " + ID);
            logger.log("Request completed");
            logger.log("Server response: No booked room for that booking ID");
            return "No booked room for that booking ID";
        }

        if (rr.getBookedBy() != null && rr.getBookedBy().equals(studentID)) {
            rr.cancelBooking();
            CentralRepository.getBookingRecord().remove(bookingID);

        } else {
            logMessage(rr.getRoomNumber(), "Cancel booking", rr.getTimeSlot(), "Can't cancel booking, room isn't booked by you!");
            return "Can't cancel booking, room isn't booked by you!";
        }

        String serverMessage ="Booking canceled on " + rr.getDate() + " at " + rr.getTimeSlot();
        logMessage(rr.getRoomNumber(), "Cancel booking", rr.getTimeSlot(), serverMessage);
        String date = rr.getDate();

        return serverMessage;
    }

    @Override
    public synchronized String changeReservation(String ID, String newCampusName, int newRoomNumber, String newTimeSlot) {
        if (newCampusName.equals(this.serverName)) {
            logger.log("Request Type: Change reservation");
            logger.log("BookingID: " + ID);
            // First check if new room number and new time slot are available, assume same date
            RoomRecord rr = null;
            String studentID = ID.split("-")[0];
            String bookingID = ID.split("-")[1];
            // Booking exists
            if (CentralRepository.getBookingRecord().containsKey(bookingID)) {
                rr = CentralRepository.getBookingRecord().get(bookingID);
            } else {
                logger.log("Request completed");
                logger.log("Server response: Impossible to change booking, bookingID does not exits!");
                return "Impossible to change booking, bookingID does not exits!";
            }

            // check if booking can be canceled
            if (!rr.getBookedBy().equals(studentID)) {
                logger.log("Request completed");
                logger.log("Server response: Can not alter booking, room is not booked by you!");
                return "Can not alter booking, room is not booked by you!";
            }

            // Check if new booking is available before cancelling previous one
            String previousDate = rr.getDate();
            if (this.roomRecords.containsKey(previousDate) &&
                    this.roomRecords.get(previousDate).containsKey(newRoomNumber)) {
                RoomRecord newRR = this.roomRecords.get(previousDate).get(newRoomNumber).get(newTimeSlot);
                // New room can be booked, cancel previous booking and book new room
                if (!newRR.isBooked()) {
                    this.cancelBooking(ID);
                    String msg = this.bookRoom(studentID, this.serverName, newRoomNumber, previousDate, newTimeSlot);
                    logger.log("Request completed");
                    logger.log("Server response: Booking changed, " + msg);
                    return "Booking changed, "  + msg;
                }
            } else {
                logger.log("Request completed");
                logger.log("Server response: Impossible to change booking, new booking not available!");
                return "Impossible to change booking, new booking not available!";
            }

            return "Could not change booking, unknown error!";
        } else {
            UdpMessage reqMessage = new UdpMessage("changeBooking", ID, newCampusName, Integer.toString(newRoomNumber), newTimeSlot, null, null);
            return udpSend(reqMessage, CentralRepository.getUdpPortNum(newCampusName));
        }
    }

    @Override
    public synchronized String getAvailableTimeSlot(String date) {
        String msg = "";
        msg += this.getEmptyRoomCount(date);

        String[] serverList = {"DVL", "WST", "KKL"};
        for (String serverName : serverList) {
            if (!serverName.equals(this.serverName)) {
                int serverPort = CentralRepository.getUdpPortNum(serverName);
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
            InetAddress aHost = InetAddress.getByName("localhost");

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
                timeSlots.put(ts, new RoomRecord(ts, rn, date, "RR" + CentralRepository.getRRIDCount()));
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
