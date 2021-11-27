package Replicas.Replica1.com;

import Replicas.Replica1.model.Booking;
import Replicas.Replica1.model.CampusID;
import Replicas.Replica1.udp.CampusUDP;
import Replicas.Replica1.udp.CampusUDPInterface;
import Replicas.Replica1.udp.UDPClient;
import Replicas.Replica1.udp.UDPServer;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import static Replicas.Replica1.model.CampusID.DVL;


@WebService(endpointInterface = "Replicas.Replica1.com.ServerInterface")
public class CampusServer implements ServerInterface {
    private static final int MAX_NUM_BOOKING = 3;
    private static final int USER_TYPE_POS = 3;

    //Variable for each separate bank server
    private CampusID campusID;

    private static ArrayList<HashMap<String, Integer>> stuBkngCntMap;

    private static int recordIdCount = 1;

    private HashMap<String, Map.Entry<String, Integer>> roomRecords;
    private HashMap<String, List<Booking>> bookingRecords;

    private Logger logger;

    private int UDPPort;
    private String UDPHost;

    //UDP Server for listening incoming requests
    private Replicas.Replica1.udp.UDPServer UDPServer;

    //Holds other servers' addresses : ["ServerName", "hostName:portNumber"]
    HashMap<String, String> serversList;

    public CampusServer() {
        this.campusID = DVL; //default value
        this.UDPHost = "localhost";
        this.UDPPort = 8080;
        this.serversList = new HashMap<>(); //default value

        this.UDPServer = new UDPServer(UDPHost, UDPPort, this);

        this.roomRecords = new HashMap<>();
        this.bookingRecords = new HashMap<>();

        stuBkngCntMap = new ArrayList<>(55);
        for (int i = 0; i < 55; i++)
            stuBkngCntMap.add(new HashMap<>());

        initiateLogger();
        this.logger.info("Initializing Server ...");


        this.logger.info("Server: " + campusID + " initialization success.");
        this.logger.info("Server: " + campusID + " port is : " + UDPPort);
    }

    public CampusServer(CampusID campusID, String host, int port, HashMap<String, String> serversList) {
        this.campusID = campusID;
        this.UDPHost = host;
        this.UDPPort = port;
        this.serversList = serversList;

        this.UDPServer = new UDPServer(UDPHost, UDPPort, this);
        new Thread(UDPServer).start();

        this.roomRecords = new HashMap<>();
        this.bookingRecords = new HashMap<>();

        stuBkngCntMap = new ArrayList<>(55);
        for (int i = 0; i < 55; i++)
            stuBkngCntMap.add(new HashMap<>());

        initiateLogger();
        this.logger.info("Initializing Server ...");


        this.logger.info("Server: " + campusID + " initialization success.");
        this.logger.info("Server: " + campusID + " port is : " + UDPPort);
    }

    private void initiateLogger() {
        Logger logger = Logger.getLogger("Server Logs/" + this.campusID.toString() + "- Server Log");
        FileHandler fh;

        try {
            //FileHandler Configuration and Format Configuration
            fh = new FileHandler("Server Logs/" + this.campusID + " - Server Log.log");

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
        String resultLog;
        resultLog = validateAdmin(adminID);
        if (resultLog != null) {
            return resultLog;
        }
        resultLog = validateDateTimeSlot(date, listOfTimeSlots);
        if (resultLog != null) {
            return resultLog;
        }

        this.logger.info(String.format("Server Log | Request: createRoom | AdminID: %s | Room number: %d | Date: %s",
                adminID, roomNumber, date));

        //TODO: null checks
        Optional<Map.Entry<String, Map.Entry<String, Integer>>> record = roomRecords.entrySet().stream()
                .filter(h -> h.getValue().getKey().equals(date) && h.getValue().getValue() == roomNumber).findFirst();
        if (record.isPresent()) {
            String recordID = record.get().getKey();
            resultLog = updateRecord(recordID, listOfTimeSlots);
        } else {
            resultLog = createRecord(roomNumber, date, listOfTimeSlots);
        }
        return resultLog;
    }

    private String createRecord(int roomNumber, String date, String[] listOfTimeSlots) {
        String resultLog;
        String recordID = "RR" + String.format("%05d", recordIdCount);
        incrementRecordID();
        while (roomRecords.get(recordID) != null) {
            incrementRecordID();
            recordID = "RR" + recordIdCount;
        }
        roomRecords.put(recordID, new AbstractMap.SimpleEntry<>(date, roomNumber));
        List<Booking> newBookings = new ArrayList<>();
        for (String slot : listOfTimeSlots) {
            newBookings.add(new Booking(recordID, null, slot));
        }
        bookingRecords.put(recordID, newBookings);
        resultLog = String.format("Server Log | Room record %s was created successfully", recordID);
        this.logger.info(resultLog);
        return resultLog;
    }

    private String updateRecord(String recordID, String[] listOfTimeSlots) {
        String resultLog;
        List<Booking> previousBookings = bookingRecords.get(recordID);
        List<Booking> newBookings = new ArrayList<>(previousBookings);
        for (String slot : listOfTimeSlots) {
            newBookings.add(new Booking(recordID, null, slot));
        }
        bookingRecords.put(recordID, newBookings);
        resultLog = String.format("Server Log | Room record %s was already created. " +
                "It was updated successfully", recordID);
        this.logger.info(resultLog);
        return resultLog;
    }

    @Override
    public String deleteRoom(String adminID, int roomNumber, String date, String[] listOfTimeSlots) {

        String resultLog;
        resultLog = validateAdmin(adminID);
        if (resultLog != null) {
            return resultLog;
        }
        resultLog = validateDateTimeSlot(date, listOfTimeSlots);
        if (resultLog != null) {
            return resultLog;
        }

        this.logger.info(String.format("Server Log | Request: deleteRoom | AdminID: %s | Room number: %d | Date: %s",
                adminID, roomNumber, date));

        //TODO: null checks
        Optional<Map.Entry<String, Map.Entry<String, Integer>>> record = roomRecords.entrySet().stream()
                .filter(h -> h.getValue().getKey().equals(date) && h.getValue().getValue() == roomNumber).findFirst();
        if (record.isPresent()) {
            String recordID = record.get().getKey();
            List<Booking> removedBookings = bookingRecords.get(recordID)
                    .stream().filter(b -> Arrays.asList(listOfTimeSlots).contains(b.getTimeslot())).collect(Collectors.toList());
            bookingRecords.get(recordID).removeIf(removedBookings::contains);
            for (Booking removedBooking : removedBookings) {
                if (removedBooking.getBookedBy() != null) {
                    //booked by the student, reducing the student's bookingCount
                    setStuBookingCnt(removedBooking.getBookedBy(), date, -1);
                }
            }
            resultLog = String.format("Server Log | Room record %s was deleted successfully", recordID);
            this.logger.info(resultLog);
        } else {
            resultLog = String.format("Server Log | ERROR: Room was not found | Request: deleteRoom | " +
                    "Room number: %d | Date: %s", roomNumber, date);
            this.logger.warning(resultLog);
        }
        return resultLog;
    }

    @Override
    public String bookRoom(String studentID, CampusID campusID, int roomNumber, String date,
                           String timeslot) {

        String resultLog;
        resultLog = validateStudent(studentID);
        if (resultLog != null) {
            return resultLog;
        }
        resultLog = validateDateTimeSlot(date, new String[]{timeslot});
        if (resultLog != null) {
            return resultLog;
        }

        //forward request to other server
        if (campusID != this.campusID) {
            try {
                this.logger.info(String.format("Server Log | Forwarding Request to %s Server: bookRoom | StudentID: %s " +
                                "| Room number: %d | Date: %s | Timeslot: %s", campusID.toString(), studentID, roomNumber,
                        date, timeslot));
                CampusServerService service = new CampusServerService();
                QName qName = new QName("http://com/", campusID.name() + "CampusServerPort");
                ServerInterface otherServer = service.getPort(qName, ServerInterface.class);
                return otherServer.bookRoom(studentID, campusID, roomNumber, date, timeslot);
            } catch (Exception e) {
                resultLog = "Server Log | Request: bookRoom | ERROR: " + campusID.toString() + " Not Bound.";
                this.logger.severe(resultLog);
                e.printStackTrace();
                return resultLog;
            }
        }
        this.logger.info(String.format("Server Log | Request: bookRoom | StudentID: %s | " +
                "Room number: %d | Date: %s | Timeslot: %s", studentID, roomNumber, date, timeslot.toString()));

        if (getStuBookingCnt(studentID, date) >= MAX_NUM_BOOKING) {
            resultLog = String.format("Server Log | ERROR: Booking limit (%d) for the week was reached | " +
                    "StudentID %s", MAX_NUM_BOOKING, studentID);
            this.logger.warning(resultLog);
            return resultLog;
        }
        //TODO: null check
        Optional<Map.Entry<String, Map.Entry<String, Integer>>> record = roomRecords.entrySet().stream()
                .filter(h -> h.getValue().getKey().equals(date) && h.getValue().getValue() == roomNumber).findFirst();
        if (record.isPresent()) {
            String recordID = record.get().getKey();
            Optional<Booking> booking = bookingRecords.get(recordID)
                    .stream().filter(b -> b.getTimeslot().equals(timeslot)).findFirst();
            if (booking.isPresent() && booking.get().getBookedBy() == null) {
                booking.get().setBookedBy(studentID);
                booking.get().setBookingID(generateBookingID(studentID, campusID, roomNumber, date, timeslot));
                setStuBookingCnt(studentID, date, 1);
                String bookingID = booking.get().getBookingID();
                resultLog = String.format("Server Log | Room record %s was booked successfully. Booking ID: %s",
                        recordID, bookingID);
                this.logger.info(resultLog);
            } else {
                resultLog = String.format("Server Log | ERROR: Time slot was not available | Request: bookRoom | " +
                        "Room number: %d | Date: %s | Timeslot: %s", roomNumber, date, timeslot.toString());
                this.logger.warning(resultLog);
            }

        } else {
            resultLog = String.format("Server Log | ERROR: Room was not found | Request: bookRoom | " +
                    "Room number: %d | Date: %s", roomNumber, date);
            this.logger.warning(resultLog);
        }
        return resultLog;
    }

    private String generateBookingID(String studentID, CampusID campusID, int roomNumber, String date, String timeslot) {
        return String.format("%s-%s-%d-%s-%s", studentID, campusID.name(), roomNumber, date, timeslot);
    }

    @Override
    public String cancelBooking(String studentID, String bookingID) {

        String resultLog;
        resultLog = validateStudent(studentID);
        if (resultLog != null) {
            return resultLog;
        }

        this.logger.info(String.format("Server Log | Request: cancelBooking | StudentID: %s | " +
                "BookingID: %s", studentID, bookingID));
        List<Booking> bookingList = bookingRecords.values().stream().flatMap(List::stream).collect(Collectors.toList());
        Optional<Booking> booking = bookingList.stream().filter(b -> b.getBookingID() != null &&
                b.getBookingID().equals(bookingID)).findFirst();
        if (booking.isPresent() && booking.get().getBookedBy().equals(studentID)) {
            booking.get().setBookedBy(null);
            booking.get().setBookingID(null);
            resultLog = String.format("Server Log | Booking %s was cancelled successfully.", bookingID);
            String date = roomRecords.get(booking.get().getRecordID()).getKey();
            setStuBookingCnt(studentID, date, -1);
            this.logger.info(resultLog);
        } else {
            resultLog = String.format("Server Log | ERROR: Booking was not found | Request: cancelBooking | BookingID: %s",
                    bookingID);
            this.logger.warning(resultLog);
        }
        return resultLog;
    }

    @Override
    public synchronized String changeReservation(String studentID, String bookingId, CampusID newCampusName, int newRoomNo,
                                                 String newTimeSlot) {
        String resultLog = validateDateTimeSlot(null, new String[]{newTimeSlot});
        if (resultLog != null) {
            return resultLog;
        }
        this.logger.info(String.format("Server Log | Request: changeReservation | StudentID: %s | " +
                        "BookingID: %s | New CampusID: %s | New room: %d | New Timeslot: %s", studentID, bookingId,
                newCampusName.name(), newRoomNo, newTimeSlot));

        //getting date from bookingId
        String date;
        List<Booking> bookingList = bookingRecords.values().stream().flatMap(List::stream).collect(Collectors.toList());
        Optional<Booking> booking = bookingList.stream().filter(b -> b.getBookingID() != null &&
                b.getBookingID().equals(bookingId)).findFirst();
        //check if booking was found in the current server
        if (booking.isPresent()) {
            Map.Entry<String, Integer> record = roomRecords.get(booking.get().getRecordID());
            date = record.getKey();
            //local-local change
            if (this.campusID == newCampusName) {
                resultLog = processLocalChange(studentID, bookingId, newCampusName, newRoomNo, newTimeSlot, date);
            } else { //local-remote change
                resultLog = processRemoteChange(studentID, bookingId, newRoomNo, newTimeSlot, newCampusName, date);
            }
        } else {
            resultLog = String.format("Server Log | ERROR: Booking was not found | Request: changeReservation | BookingID: %s",
                    bookingId);
            this.logger.warning(resultLog);
        }
        return resultLog;
    }

    private String processRemoteChange(String studentID, String bookingId, int newRoomNo, String newTimeSlot,
                                       CampusID newCampusID, String date) {
        String resultLog = null;
        List<Booking> bookingList = bookingRecords.values().stream().flatMap(List::stream).collect(Collectors.toList());
        Optional<Booking> originalBooking = bookingList.stream().filter(b -> b.getBookingID() != null &&
                b.getBookingID().equals(bookingId)).findFirst();
        String cancelBookingStatus = cancelBooking(studentID, bookingId);
        if (cancelBookingStatus.contains("success")) {
            // Loop through the serversList to find the information of the remote server
            for (String remoteCampusID : serversList.keySet()) {
                if (newCampusID.name().equals(remoteCampusID)) {
                    this.logger.info("Server Log: | Change Reservation Log: | Connection Initialized.");

                    //3.1 Extract the key that is associated with the destination branch.
                    String connectionData = serversList.get(newCampusID.name());

                    //3.2 Extract the host and IP [host:IP]
                    String hostDest = connectionData.split(":")[0];
                    int portDest = Integer.parseInt(connectionData.split(":")[1]);

                    //3.3 Create an UDPClient and prepare the request.
                    UDPClient requestClient = new UDPClient(hostDest, portDest, campusID);

                    CampusUDPInterface transferReq = new CampusUDP(studentID, newCampusID, newRoomNo, newTimeSlot, date);
                    requestClient.send(transferReq);

                    //3.4 Receive the response.
                    CampusUDPInterface transferResp = requestClient.getResponse();

                    //3.5 IF successfully transfer ...
                    if (((CampusUDP) transferResp).isTransferStatus()) {
                        resultLog = String.format("Server Log | Booking was changed successfully successfully. " +
                                        "StudentID: %s | BookingID: %s | New CampusID: %s | New room: %d | New Timeslot: %s",
                                studentID, bookingId, newCampusID.name(), newRoomNo, newTimeSlot.toString());
                        this.logger.info(resultLog);
                    } else {
                        //We can't cancel for some reason. Book the original room back
                        rebookOriginalRoom(originalBooking.get(), studentID, date);
                        resultLog = String.format("Server Log | ERROR Request: changeReservation | BookingID: %s",
                                bookingId);
                    }
                }
            }
        } else {
            resultLog = String.format("Server Log | ERROR Request: changeReservation | BookingID: %s\n ERROR:",
                    bookingId);
            resultLog += cancelBookingStatus;
            this.logger.warning(resultLog);
        }
        return resultLog;
    }

    private String processLocalChange(String studentID, String bookingId, CampusID newCampusName,
                                      int newRoomNo, String newTimeSlot, String date) {
        String resultLog;
        List<Booking> bookingList = bookingRecords.values().stream().flatMap(List::stream).collect(Collectors.toList());
        Optional<Booking> originalBooking = bookingList.stream().filter(b -> b.getBookingID() != null &&
                b.getBookingID().equals(bookingId)).findFirst();

        String cancelStatus = cancelBooking(studentID, bookingId);
        if (cancelStatus.contains("success")) {
            String bookingStatus = bookRoom(studentID, newCampusName, newRoomNo, date, newTimeSlot);
            if (bookingStatus.contains("success")) {
                resultLog = String.format("Server Log | Booking was changed successfully successfully. " +
                                "StudentID: %s | BookingID: %s | New CampusID: %s | New room: %d | New Timeslot: %s",
                        studentID, bookingId, campusID.name(), newRoomNo, newTimeSlot.toString());
                this.logger.info(resultLog);
            } else {
                //We can't book for some reason. Book the original room back
                rebookOriginalRoom(originalBooking.get(), studentID, date);
                resultLog = String.format("Server Log | ERROR Request: changeReservation | BookingID: %s\n ERROR:",
                        bookingId);
                resultLog += bookingStatus;
            }
        } else {
            //We can't cancel for some reason. Book the original room back
            resultLog = String.format("Server Log | ERROR Request: changeReservation | BookingID: %s\n ERROR:",
                    bookingId);
            resultLog += cancelStatus;
        }
        return resultLog;
    }

    private void rebookOriginalRoom(Booking booking, String studentID, String date) {
        //Getting the original booking info from bookingID:
        this.logger.warning("Server Log | ERROR Request: changeReservation | Changing booking wasn't successful, rebooking original room. ");
        Map.Entry<String, Integer> record = roomRecords.get(booking.getRecordID());
        int roomNum = record.getValue();
        bookRoom(studentID, CampusID.valueOf(this.campusID.name()),
                roomNum, date, booking.getTimeslot());
    }

    @Override
    public String getAvailableTimeSlot(String date) {
        this.logger.info(String.format("Server Log | Request: getAvailableTimeSlot | Date: %s", date));

        HashMap<CampusID, Integer> totalTimeSlotCount = new HashMap<>();
        int localTimeSlotCount = getLocalAvailableTimeSlot();
        totalTimeSlotCount.put(this.campusID, localTimeSlotCount);
        String resultLog;

        //1. Create UDP Socket
        for (String campusServer : serversList.keySet()) {
            if (campusServer.equals(this.campusID.toString())) {
                continue;
            }

            //3.1 Extract the key that is associated with the destination branch.
            String connectionData = serversList.get(campusServer);

            //3.2 Extract the host and IP [host:IP]
            String hostDest = connectionData.split(":")[0];
            int portDest = Integer.parseInt(connectionData.split(":")[1]);

            //3.3 Create an UDPClient and prepare the request.
            UDPClient requestClient = new UDPClient(hostDest, portDest, CampusID.valueOf(campusServer));

            CampusUDPInterface timeslotReq = new CampusUDP();
            requestClient.send(timeslotReq);

            //3.4 Receive the response.
            CampusUDPInterface timeslotResp = requestClient.getResponse();
            int rData = 0;
            //3.5 IF successfully transfer ...
            if (((CampusUDP) timeslotResp).isTransferStatus()) {
                rData = ((CampusUDP) timeslotResp).getLocalAvailableTimeSlot();
            } else {
                this.logger.warning(String.format("Server Log | getAvailableTimeSlot() ERROR: Unable to get available " +
                        "slots for Campus: %s", campusServer));
            }

            totalTimeSlotCount.put(CampusID.valueOf(campusServer), rData);
            resultLog = "Server Log | Getting the available timeslots was successful.";
            this.logger.info(resultLog);
        }

        return totalTimeSlotCount.toString();
    }

    @Override
    public int getLocalAvailableTimeSlot() {
        List<Booking> bookingList = bookingRecords.values().stream().flatMap(List::stream).collect(Collectors.toList());
        List<Booking> nullBookings = bookingList.stream().filter(b -> b.getBookedBy() == null).collect(Collectors.toList());
        return nullBookings.size();
    }

    private int getStuBookingCnt(String studentID, String d) {
        Calendar cal = Calendar.getInstance();
        Date date = null;
        try {
            date = new SimpleDateFormat("dd/MM/yyyy").parse(d);
        } catch (ParseException e) {
            e.printStackTrace(); //TODO: better handling
        }
        cal.setTime(date);
        int week = cal.get(Calendar.WEEK_OF_YEAR);

        HashMap<String, Integer> stuMap = stuBkngCntMap.get(week);
        Integer cnt = stuMap.get(studentID);
        if (cnt == null)
            return 0;
        else
            return cnt;
    }

    private void setStuBookingCnt(String studentID, String d, int offset) {
        Calendar cal = Calendar.getInstance();
        Date date = null;
        try {
            date = new SimpleDateFormat("dd/MM/yyyy").parse(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        cal.setTime(date);
        int week = cal.get(Calendar.WEEK_OF_YEAR);

        HashMap<String, Integer> stuMap = stuBkngCntMap.get(week);
        Integer cnt = stuMap.get(studentID);
        if (cnt == null) cnt = 0;
        if (offset > 0) {
            stuMap.put(studentID, ++cnt);
        } else {
            stuMap.put(studentID, --cnt);
        }
    }

    private synchronized static void incrementRecordID() {
        recordIdCount++;
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

    private String validateDateTimeSlot(String date, String[] listOfTimeSlots) {
        if (listOfTimeSlots != null) {
            for (String slot : listOfTimeSlots) {
                // expecting "12:00-13:00"
                double start = Double.parseDouble(slot.substring(0, slot.indexOf("-")).replace(":",""))/100;
                double end = Double.parseDouble(slot.substring(slot.indexOf("-")+1).replace(":",""))/100;
                if (start < 0 || start >= 24 || end < 0 || end >= 24 || start >= end) {
                    return "Invalid timeslot format. Use the 24h clock. Format: '12:00-13:00'";
                }
            }
        }
        if (date != null) {
            try {
                new SimpleDateFormat("dd/MM/yyyy").parse(date);
            } catch (ParseException e) {
                return "Invalid date format.";
            }
        }
        return null;
    }

    public CampusID getCampusID() {
        return this.campusID;
    }
}
