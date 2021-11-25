package Replicas;

public interface CampusServerInterface extends Runnable {
    //ADMIN ONLY
    String createRoom(String adminID, int roomNumber, String date, String[] listOfTimeSlots);
    String deleteRoom(String adminID, int roomNumber, String date, String[] listOfTimeSlots);

    //STUDENT ONLY
    String bookRoom(String studentID, String campusID, int roomNumber, String date, String timeSlot);
    String getAvailableTimeSlot(String date);
    String cancelBooking(String studentID, String bookingID);
    String changeReservation(String studentID, String bookingId, String newCampusName, int newRoomNo, String newTimeSlot);
}
