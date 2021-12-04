package Client;

public class StudentDriver {

    public static void main(String[] args) {
        String sid1 = "DVLS1234";
        String sid2 = "DVLS2234";
        String sid3 = "DVLA1234";
        String sid4 = "DVLD1234";

        String timeSlot = "1:00-2:00";
        String timeSlot2 = "2:00-15:00";
        String timeSlot3 = "12:00-13:00";
        String invalidTimeSlot = "26:00-20:00";

        try {
            StudentClient testClient1 = new StudentClient(sid1);
            StudentClient testClient2 = new StudentClient(sid2);

            //Invalid client tests:
//            StudentClient adminClient = new StudentClient(sid3);
//            StudentClient invalidClient = new StudentClient(sid4);

            String serverTimeSlot = testClient1.getAvailableTimeSlot("03/01/2020");
            System.out.println(serverTimeSlot);

//            testing invalid parameters:
//            testClient1.bookRoom("KKL", 201, "03/01/2020", invalidTimeSlot);
//            adminClient.bookRoom("KKL", 201, "03/01/2020", timeSlot);

            //testing max booking for student
            testClient1.bookRoom("KKL", 201, "03/01/2020", timeSlot);
//            testClient1.bookRoom("WST", 211, "04/01/2020", timeSlot);
//            String bookingID = testClient1.bookRoom("DVL", 203, "01/01/2020", timeSlot);
//            if (bookingID != null) {
//                testClient1.changeReservation(bookingID, "KKL", (short) 201, timeSlot);
//            }
//
//            testClient1.bookRoom("DVL", 203, "01/01/2020", timeSlot3);

//            testClient2.cancelBooking(bookingID); //shouldn't work since it's not the student who booked
//            testClient1.cancelBooking(bookingID);
//            testClient1.bookRoom(DVL, 203, "01/01/2020", timeSlot3);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}