package Replicas.Replica3.campus;

public interface CampusInterface {
    String createRoom(int roomNumber, String date, String[] timeSlots);
    String deleteRoom(int roomNumber, String date, String[] timeSlots);
    String bookRoom(String studentID, String campusName, int roomNumber, String date, String timeSlot);
    String cancelBooking(String bookingID);
    String getAvailableTimeSlot(String date);
    String changeReservation(String bookingID, String newCampusName, int newRoomNumber, String newTimeSlot);
}
