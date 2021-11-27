package Replicas.Replica1.udp;


import Replicas.Replica1.com.ServerInterface;
import Replicas.Replica1.model.CampusID;

public class CampusUDP implements CampusUDPInterface {
    private static final long serialVersionUID = 1L;

    private String studentID;
    private int newRoomNo;
    private String newTimeSlot;
    private CampusID newCampusID;
    private String date;
    private String operationType;
    private boolean transferStatus = false;
    private int availableTimeSlot;

    //Constructor for inter-campus booking change
    public CampusUDP(String studentID, CampusID newCampusName, int newRoomNo, String newTimeSlot, String date) {
        this.studentID = studentID;
        this.newCampusID = newCampusName;
        this.newRoomNo = newRoomNo;
        this.newTimeSlot = newTimeSlot;
        this.date = date;
        this.operationType = "bookingChange";
    }

    //Constructor for get total available time slots.
    public CampusUDP() {
        this.operationType = "getAvailableTimeSlots";
    }

    public boolean isTransferStatus() {
        return transferStatus;
    }

    public int getLocalAvailableTimeSlot() {
        return availableTimeSlot;
    }

    @Override
    public void execute(ServerInterface campusServer, CampusID campusID) {
        try {
            if (this.operationType.equals("bookingChange")) {
                String resultLog = campusServer.bookRoom(studentID, newCampusID, newRoomNo, date, newTimeSlot);
                transferStatus = resultLog.contains("success");
            } else if (this.operationType.equals("getAvailableTimeSlots")) {
                availableTimeSlot = campusServer.getLocalAvailableTimeSlot();
                transferStatus = true;
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}