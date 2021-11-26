package Replicas.Replica3.campus;

import java.util.Locale;
import java.util.UUID;

public class RoomRecord {
    private String timeSlot;
    private String RecordID;
    private String bookedBy;
    private int roomNumber;
    private String date;

    public RoomRecord(String timeSlot, int roomNumber, String date, String RecordID) {
        this.roomNumber = roomNumber;
        this.timeSlot = timeSlot;
        this.bookedBy = null;
        this.date = date;
        this.RecordID = RecordID;
    };

    public String getStatus() {
        return "Room " + roomNumber + " booked by " + (bookedBy == null ? "nobody" : bookedBy) + " at " + timeSlot;
    }

    public boolean isBooked() {
        return this.bookedBy != null;
    }

    public String getBookedBy() {
        return this.bookedBy;
    }

    public String book(String studentID) {
        this.bookedBy = studentID;
        return UUID.randomUUID().toString().replaceAll("-", "".toUpperCase(Locale.ROOT));
    }

    public void cancelBooking() {
        this.bookedBy = null;
    }

    public String getDate() {
        return this.date;
    }

    public int getRoomNumber() {
        return this.roomNumber;
    }

    public String getTimeSlot() {
        return this.timeSlot;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof RoomRecord)) {
            return false;
        }

        RoomRecord r = (RoomRecord) o;
        if (this.bookedBy != null) {
            return (this.bookedBy.equals(r.bookedBy)
                    && this.roomNumber == r.roomNumber && this.timeSlot.equals(r.timeSlot) && this.date == r.date);
        }

        return false;
    }
}
