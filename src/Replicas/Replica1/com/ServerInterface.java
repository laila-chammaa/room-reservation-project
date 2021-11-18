package Replicas.Replica1.com;

import Replicas.Replica1.model.CampusID;
import Replicas.Replica1.model.Timeslot;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface ServerInterface {
    //ADMIN ONLY
    String createRoom(String adminID, int roomNumber, String date, Timeslot[] listOfTimeSlots);
    String deleteRoom(String adminID, int roomNumber, String date, Timeslot[] listOfTimeSlots);

    //STUDENT ONLY
    String bookRoom(String studentID, CampusID campusID, int roomNumber, String date, Timeslot timeSlot);
    String getAvailableTimeSlot(String date);
    String cancelBooking(String studentID, String bookingID);
    String changeReservation(String studentID, String bookingId, CampusID newCampusName, int newRoomNo, Timeslot newTimeSlot);

    int getLocalAvailableTimeSlot();
}
