package Replicas.Replica1.model;

import java.util.Objects;

public class Booking {
    private String recordID;
    private String bookedBy; //studentID if it’s booked by student, null otherwise
    private String bookingID;
    private String timeslot;

    public Booking(String recordID, String bookedBy, String timeslot) {
        this.recordID = recordID;
        this.bookedBy = bookedBy;
        this.timeslot = timeslot;
    }

    public Booking(String recordID, String bookedBy, String timeslot, String bookingID) {
        this.recordID = recordID;
        this.bookedBy = bookedBy;
        this.timeslot = timeslot;
        this.bookingID = bookingID;
    }

    public String getRecordID() {
        return recordID;
    }

    public void setRecordID(String recordID) {
        this.recordID = recordID;
    }

    public String getBookedBy() {
        return bookedBy;
    }

    public void setBookedBy(String bookedBy) {
        this.bookedBy = bookedBy;
    }

    public String getBookingID() {
        return bookingID;
    }

    public void setBookingID(String bookingID) {
        this.bookingID = bookingID;
    }

    public String getTimeslot() {
        return timeslot;
    }

    public void setTimeslot(String timeslot) {
        this.timeslot = timeslot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(recordID, booking.recordID) &&
                Objects.equals(bookedBy, booking.bookedBy) &&
                Objects.equals(bookingID, booking.bookingID) &&
                Objects.equals(timeslot, booking.timeslot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordID, bookedBy, bookingID, timeslot);
    }

    @Override
    public String toString() {
        return "Booking{" +
                "recordID='" + recordID + '\'' +
                ", bookedBy='" + bookedBy + '\'' +
                ", bookingID='" + bookingID + '\'' +
                ", timeslot=" + timeslot +
                '}';
    }
}
