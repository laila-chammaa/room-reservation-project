package Test;

import DRRS.MessageKeys;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collections;

public class TestHelpers {
    static JSONArray buildTimeSlotArray(String[] timeSlots) {
        JSONArray timeslotArray = new JSONArray();
        Collections.addAll(timeslotArray, timeSlots);

        return timeslotArray;
    }
}
