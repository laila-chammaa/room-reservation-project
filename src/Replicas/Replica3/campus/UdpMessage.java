package Replicas.Replica3.campus;

public class UdpMessage implements UdpMessageInterface {
    private String opName;
    private String ID;
    private String campusName;
    private String roomNumber;
    private String timeSlot;
    private String[] timeSlots;
    private String date;
    private String bookingID;

    public UdpMessage(String opName, String ID, String campusName, String roomNumber, String timeSlot, String date, String bookingID) {
        this.opName = opName;
        this.ID = ID;
        this.campusName = campusName;
        this.roomNumber = roomNumber;
        this.timeSlot = timeSlot;
        this.date = date;
        this.bookingID = bookingID;
    }

    public UdpMessage(String opName, String ID, String campusName, String roomNumber, String[] timeSlots, String date, String bookingID) {
        this.opName = opName;
        this.ID = ID;
        this.campusName = campusName;
        this.roomNumber = roomNumber;
        this.timeSlots = new String[timeSlots.length];
        System.arraycopy(timeSlots, 0, this.timeSlots, 0, timeSlots.length);
        this.date = date;
        this.bookingID = bookingID;
    }

    @Override
    public String getOpName() {
        return opName;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getCampusName() {
        return campusName;
    }

    @Override
    public String getRoomNumber() {
        return roomNumber;
    }

    @Override
    public String getTimeSlot() {
        return timeSlot;
    }

    @Override
    public String[] getTimeSlots() {
        return timeSlots;
    }

    @Override
    public String getDate() {
        return date;
    }

    @Override
    public String getBookingID() {
        return bookingID;
    }

    @Override
    public String toString() {
        if (timeSlot == null) {
            StringBuilder times = new StringBuilder();
            for (String ts : timeSlots) {
                times.append(ts);
            }

            return opName + "_" + ID + "_" + campusName + "_" + roomNumber + "_" + times.toString();
        } else {
            return opName + "_" + ID + "_" + campusName + "_" + roomNumber + "_" + timeSlot;
        }

    }
}
