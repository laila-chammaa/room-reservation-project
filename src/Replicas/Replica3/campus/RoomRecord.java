package Replicas.Replica3.campus;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.UUID;

public class RoomRecord {
    private String timeSlot;
    private String RecordID;
    private String bookedBy;
    private int roomNumber;
    private String date;
    private String bookingID;
    private String campus;

    public RoomRecord(String timeSlot, int roomNumber, String date, String RecordID, String campus) {
        this.roomNumber = roomNumber;
        this.timeSlot = timeSlot;
        this.bookedBy = "";
        this.date = date;
        this.RecordID = RecordID;
        this.bookingID = "";
        this.campus = campus;
    };

    public String getStatus() {
        return "Room " + roomNumber + " booked by " + (bookedBy == null ? "nobody" : bookedBy) + " at " + timeSlot;
    }

    public boolean isBooked() {
        if (this.bookedBy != null && this.bookedBy.equals(""))
            return false;
        return true;
    }

    public String getBookedBy() {
        return this.bookedBy;
    }

    public String book(String studentID) {
        this.bookedBy = studentID;
        this.bookingID = MessageFormat.format("{0}-{1}-{2}-{3}-{4}", bookedBy, campus, roomNumber, date, timeSlot);
        return bookingID;
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

    public String getBookingID() {
        return this.bookingID;
    }

    public void setBookingID(String bookingID) {
        this.bookingID = bookingID;
    }

    public void setBookedBy(String bookedBy) {
        this.bookedBy = bookedBy;
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
