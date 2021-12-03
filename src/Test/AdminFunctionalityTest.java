package Test;

import DRRS.Config;
import DRRS.MessageKeys;
import DRRS.ReplicaManagerDriver;

import DRRS.Sequencer;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.*;

import java.io.IOException;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminFunctionalityTest {
    @BeforeAll
    public static void setup() throws IOException{
        String[] args = {};
        Sequencer.main(args);
        ReplicaManagerDriver.main(args);
    }

    @Test
    @Order(1)
    public void replicaCreateRoomTest() throws ParseException, InterruptedException {
        String[] timeSlots = {"10:00-11:00", "18:00-19:00", "19:00-20:00", "20:00-21:00"};
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
        JSONObject res = seq.sendMessage(msgJson);
        Assertions.assertEquals("SUCCESS", res.get("status_code").toString());
    }

    @Test
    @Order(2)
    public void replicaDeleteRoomTest() {
        String[] timeSlots = {"10:00-11:00", "18:00-19:00", "19:00-20:00", "20:00-21:00"};
        JSONObject msgJson = new JSONObject();
        msgJson.put(MessageKeys.COMMAND_TYPE, Config.DELETE_ROOM);
        msgJson.put(MessageKeys.CAMPUS, "KKL");
        msgJson.put(MessageKeys.ADMIN_ID, "KKLA1234");
        msgJson.put(MessageKeys.ROOM_NUM, 101);
        msgJson.put(MessageKeys.DATE, "01/01/2022");
        msgJson.put(MessageKeys.TIMESLOT, "10:00-11:00");
        msgJson.put(MessageKeys.MESSAGE_ID, 2);
        msgJson.put(MessageKeys.TIMESLOTS, TestHelpers.buildTimeSlotArray(timeSlots));

        SendToSequencer seq = new SendToSequencer();
        JSONObject res = seq.sendMessage(msgJson);
        Assertions.assertEquals("SUCCESS", res.get("status_code").toString());
    }

    @Test
    @Order(3)
    public void replicaTestUnauthorized() {
        String[] timeSlots = {"10:00-11:00", "18:00-19:00", "19:00-20:00", "20:00-21:00"};
        JSONObject msgJson = new JSONObject();
        msgJson.put(MessageKeys.COMMAND_TYPE, Config.CREATE_ROOM);
        msgJson.put(MessageKeys.CAMPUS, "KKL");
        msgJson.put(MessageKeys.ADMIN_ID, "KKLS1234");
        msgJson.put(MessageKeys.ROOM_NUM, 101);
        msgJson.put(MessageKeys.DATE, "01/01/2022");
        msgJson.put(MessageKeys.TIMESLOT, "10:00-11:00");
        msgJson.put(MessageKeys.MESSAGE_ID, 3);
        msgJson.put(MessageKeys.TIMESLOTS, TestHelpers.buildTimeSlotArray(timeSlots));

        SendToSequencer seq = new SendToSequencer();
        JSONObject res = seq.sendMessage(msgJson);
        Assertions.assertTrue(res.get("message") != null && (res.get("message").toString().contains("authenticate")
                || res.get("message").toString().contains("authorized") || res.get("message").toString().contains("admins only")));
    }

    @Test
    @Order(4)
    public void replicaRecordNonExistent() {
        String[] timeSlots = {"22:00-23:00", "10:00-11:35"};
        JSONObject msgJson = new JSONObject();
        msgJson.put(MessageKeys.COMMAND_TYPE, Config.DELETE_ROOM);
        msgJson.put(MessageKeys.CAMPUS, "KKL");
        msgJson.put(MessageKeys.ADMIN_ID, "KKLA1234");
        msgJson.put(MessageKeys.ROOM_NUM, 101);
        msgJson.put(MessageKeys.DATE, "01/01/2022");
        msgJson.put(MessageKeys.TIMESLOT, "10:00-11:00");
        msgJson.put(MessageKeys.MESSAGE_ID, 4);
        msgJson.put(MessageKeys.TIMESLOTS, TestHelpers.buildTimeSlotArray(timeSlots));

        SendToSequencer seq = new SendToSequencer();
        JSONObject res = seq.sendMessage(msgJson);
        Assertions.assertEquals("FAIL", res.get("status_code").toString());
    }
}