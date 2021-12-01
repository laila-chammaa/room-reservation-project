package Frontend;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface FrontendInterface {
    //ADMIN ONLY
    String createRoom(String adminID, int roomNumber, String date, String[] listOfTimeSlots);
    String deleteRoom(String adminID, int roomNumber, String date, String[] listOfTimeSlots);

    //STUDENT ONLY
    String bookRoom(String studentID, String campusID, int roomNumber, String date, String timeSlot);
    String getAvailableTimeSlot(String studentId, String date);
    String cancelBooking(String studentID, String bookingID);
    String changeReservation(String studentID, String bookingId, String newCampusName, int newRoomNo, String newTimeSlot);

    void shutdown();
}
