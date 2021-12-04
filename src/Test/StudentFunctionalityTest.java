package Test;

import DRRS.Config;
import DRRS.MessageKeys;
import DRRS.ReplicaManagerDriver;
import DRRS.Sequencer;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.*;

import java.io.IOException;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StudentFunctionalityTest {
    @BeforeAll
    public static void setup() throws IOException{
        String[] args = {};
        Sequencer.main(args);
        ReplicaManagerDriver.main(args);

        // Creating some records
        String[] timeSlots = {"10:00-11:00", "11:00-12:00", "12:00-13:00", "18:00-19:00", "19:00-20:00", "20:00-21:00"};
        JSONObject msgJson = new JSONObject();
        msgJson.put(MessageKeys.COMMAND_TYPE, Config.CREATE_ROOM);
        msgJson.put(MessageKeys.CAMPUS, "KKL");
        msgJson.put(MessageKeys.ADMIN_ID, "KKLA1234");
        msgJson.put(MessageKeys.ROOM_NUM, 101);
        msgJson.put(MessageKeys.DATE, "01/01/2022");
        msgJson.put(MessageKeys.TIMESLOT, "10:00-11:00");
        msgJson.put(MessageKeys.MESSAGE_ID, 1);
        msgJson.put(MessageKeys.TIMESLOTS, TestHelpers.buildTimeSlotArray(timeSlots));

        SendToSequencer seq = new SendToSequencer();
        seq.sendMessage(msgJson);
    }

    @Test
    @Order(1)
    public void replicaBookingTest() {
        JSONObject msgJson = new JSONObject();
        msgJson.put(MessageKeys.COMMAND_TYPE, Config.BOOK_ROOM);
        msgJson.put(MessageKeys.CAMPUS, "KKL");
        msgJson.put(MessageKeys.STUDENT_ID, "KKLS1234");
        msgJson.put(MessageKeys.ROOM_NUM, 101);
        msgJson.put(MessageKeys.DATE, "01/01/2022");
        msgJson.put(MessageKeys.TIMESLOT, "12:00-13:00");
        msgJson.put(MessageKeys.MESSAGE_ID, 2);

        SendToSequencer seq = new SendToSequencer();
        JSONObject res = seq.sendMessage(msgJson);
        Assertions.assertEquals("SUCCESS", res.get("status_code").toString());
    }

    @Test
    @Order(2)
    public void replicaChangeReservationTest() {
        String bookingID = "KKLS1234-KKL-101-01/01/2022-12:00-13:00";
        JSONObject msgJson = new JSONObject();
        msgJson.put(MessageKeys.COMMAND_TYPE, Config.CHANGE_RESERVATION);
        msgJson.put(MessageKeys.CAMPUS, "KKL");
        msgJson.put(MessageKeys.STUDENT_ID, "KKLS1234");
        msgJson.put(MessageKeys.BOOKING_ID, bookingID);
        msgJson.put(MessageKeys.ROOM_NUM, 101);
        msgJson.put(MessageKeys.DATE, "01/01/2022");
        msgJson.put(MessageKeys.TIMESLOT, "18:00-19:00");
        msgJson.put(MessageKeys.MESSAGE_ID, 3);

        SendToSequencer seq = new SendToSequencer();
        JSONObject res = seq.sendMessage(msgJson);
        Assertions.assertEquals("SUCCESS", res.get("status_code").toString());
    }

    @Test
    @Order(3)
    public void replicaCancelBookingTest() {
        String bookingID = "KKLS1234-KKL-101-01/01/2022-18:00-19:00";
        JSONObject msgJson = new JSONObject();
        msgJson.put(MessageKeys.COMMAND_TYPE, Config.CANCEL_BOOKING);
        msgJson.put(MessageKeys.STUDENT_ID, "KKLS1234");
        msgJson.put(MessageKeys.MESSAGE_ID, 4);
        msgJson.put(MessageKeys.BOOKING_ID, bookingID);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SendToSequencer seq = new SendToSequencer();
        JSONObject res = seq.sendMessage(msgJson);
        Assertions.assertEquals("SUCCESS", res.get("status_code").toString());
    }

    @Test
    @Order(4)
    public void replicaGetTimeSlotsTest() {
        JSONObject msgJson = new JSONObject();
        msgJson.put(MessageKeys.COMMAND_TYPE, Config.GET_TIMESLOTS);
        msgJson.put(MessageKeys.STUDENT_ID, "KKLS1234");
        msgJson.put(MessageKeys.DATE, "01/01/2022");
        msgJson.put(MessageKeys.MESSAGE_ID, 5);

        SendToSequencer seq = new SendToSequencer();
        JSONObject res = seq.sendMessage(msgJson);
        System.out.println("MY MESSAGE");
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String ans = (res.get("message").toString());
        Assertions.assertEquals("SUCCESS", res.get("status_code").toString());
        Assertions.assertTrue(res.get("message").toString().contains("KKL 6") || res.get("message").toString().contains("Getting the available timeslots was successful"));

    }

    @Test
    @Order(5)
    public void replicaOverbookingTestTest() {
        JSONObject msgJson = new JSONObject();
        msgJson.put(MessageKeys.COMMAND_TYPE, Config.BOOK_ROOM);
        msgJson.put(MessageKeys.CAMPUS, "KKL");
        msgJson.put(MessageKeys.STUDENT_ID, "KKLS1234");
        msgJson.put(MessageKeys.ROOM_NUM, 101);
        msgJson.put(MessageKeys.DATE, "01/01/2022");
        msgJson.put(MessageKeys.TIMESLOT, "11:00-12:00");
        msgJson.put(MessageKeys.MESSAGE_ID, 8);

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SendToSequencer seq = new SendToSequencer();
        seq.sendMessage(msgJson);

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        msgJson.put(MessageKeys.TIMESLOT, "10:00-11:00");
        msgJson.put(MessageKeys.MESSAGE_ID, 9);
        seq.sendMessage(msgJson);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        msgJson.put(MessageKeys.TIMESLOT, "20:00-21:00");
        msgJson.put(MessageKeys.MESSAGE_ID, 10);
        seq.sendMessage(msgJson);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        msgJson.put(MessageKeys.TIMESLOT, "11:00-12:00");
        msgJson.put(MessageKeys.MESSAGE_ID, 11);
        JSONObject res = seq.sendMessage(msgJson);
        Assertions.assertEquals("FAIL", res.get("status_code").toString());
    }
}
