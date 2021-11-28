package Replicas.Replica3.campus;

public interface CampusInterface {
    String createRoom(String adminID, int roomNumber, String date, String[] timeSlots);
    String deleteRoom(String adminID, int roomNumber, String date, String[] timeSlots);

    String bookRoom(String studentID, String campusName, int roomNumber, String date, String timeSlot);
    String cancelBooking(String studentID, String bookingID);
    String getAvailableTimeSlot(String date);
    String changeReservation(String studentID, String bookingID, String newCampusName, int newRoomNumber, String newTimeSlot);
}
