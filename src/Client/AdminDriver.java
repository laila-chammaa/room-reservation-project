package Client;

import Client.com.net.java.dev.jaxb.array.StringArray;

import java.util.Arrays;
import java.util.List;

public class AdminDriver {
    public static void main(String[] args) {
        StringArray listOfTimeSlots = new StringArray();
        listOfTimeSlots.getItem().addAll(Arrays.asList("19:00-20:00", "12:00-13:00", "15:00-16:00"));
        StringArray listOfTimeSlots2 = new StringArray();
        listOfTimeSlots2.getItem().add("1:00-2:00");
        StringArray listOfTimeSlots3 = new StringArray();
        listOfTimeSlots3.getItem().add("15:00-16:00");


        String aid1 = "KKLA1234";
        String aid2 = "WSTA1234";
        String aid3 = "DVLA1234";
        String aid4 = "KKLS1214";

        try {
            AdminClient testClient1 = new AdminClient(aid1);
            AdminClient testClient2 = new AdminClient(aid2);
            AdminClient testClient3 = new AdminClient(aid3);

            //testing synchronization with multiple admins
            testClient1.createRoom(201, "01/01/2020", listOfTimeSlots);
            testClient1.createRoom(201, "01/02/2020", listOfTimeSlots);
            testClient1.createRoom(201, "01/03/2020", listOfTimeSlots);
            testClient3.createRoom(201, "01/01/2020", listOfTimeSlots);
            testClient1.createRoom(201, "03/01/2020", listOfTimeSlots2);
            testClient2.createRoom(211, "04/01/2020", listOfTimeSlots);
            testClient2.deleteRoom(211, "04/01/2020", listOfTimeSlots3);
            testClient3.createRoom(203, "01/01/2020", listOfTimeSlots);


//            testing invalid admin
//            AdminClient studentClient = new AdminClient(args, aid4);
//            studentClient.createRoom((short) 201, "01/01/2020", listOfTimeSlots);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
