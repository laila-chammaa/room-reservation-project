package Frontend;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface FrontendInterface {
    //ADMIN ONLY
    @WebMethod
    String createRoom(String adminID, int roomNumber, String date, String[] listOfTimeSlots);
    @WebMethod
    String deleteRoom(String adminID, int roomNumber, String date, String[] listOfTimeSlots);

    //STUDENT ONLY
    @WebMethod
    String bookRoom(String studentID, String campusID, int roomNumber, String date, String timeSlot);
    @WebMethod
    String getAvailableTimeSlot(String studentId, String date);
    @WebMethod
    String cancelBooking(String studentID, String bookingID);
    @WebMethod
    String changeReservation(String studentID, String bookingId, String newCampusName, int newRoomNo, String newTimeSlot);

    @WebMethod
    void shutdown();
}
