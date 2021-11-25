package Replicas.Replica2;

import java.io.Serializable;
import java.util.UUID;

/**
 * Room Record object
 */
public class RoomRecordBooking implements Serializable {
	public String timeSlot;
	public String bookedBy;
	public String bookingId;
	
	public RoomRecordBooking(String timeSlot) {
		this(timeSlot, null, null);
	}
	
	public RoomRecordBooking(String timeSlot, String bookedBy, String bookingId) {
		this.timeSlot = timeSlot;
		this.bookedBy = bookedBy;
		this.bookingId = bookingId;
	}
	
	public String book(String bookedBy, String campus) {
		this.bookedBy = bookedBy;
		this.bookingId = campus + "-" + UUID.randomUUID().toString();
		return this.bookingId;
	}

	public String bookWithId(String bookedBy, String bookingId) {
		this.bookedBy = bookedBy;
		this.bookingId = bookingId;
		return this.bookingId;
	}
	
	public void cancel() {
		this.bookedBy = null;
		this.bookingId = null;
	}
	
	@Override
	public String toString() {
		return "RoomRecordBooking{" +
				"timeSlot='" + timeSlot + '\'' +
				", bookedBy='" + bookedBy + '\'' +
				", bookingId=" + bookingId +
				'}';
	}
}
