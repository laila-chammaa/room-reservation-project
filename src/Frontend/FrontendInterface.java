package Frontend;

import Replicas.Replica1.model.CampusID;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface FrontendInterface {
    //ADMIN ONLY
    String createRoom(String adminID, int roomNumber, String date, String[] listOfTimeSlots);
    String deleteRoom(String adminID, int roomNumber, String date, String[] listOfTimeSlots);

    //STUDENT ONLY
    String bookRoom(String studentID, CampusID campusID, int roomNumber, String date, String timeSlot);
    String getAvailableTimeSlot(String date);
    String cancelBooking(String studentID, String bookingID);
    String changeReservation(String studentID, String bookingId, CampusID newCampusName, int newRoomNo, String newTimeSlot);

    void shutdown();
}
