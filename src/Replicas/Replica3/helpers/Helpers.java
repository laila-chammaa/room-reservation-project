package Replicas.Replica3.helpers;

import Replicas.Replica3.campus.RoomRecord;
import Replicas.Replica3.utils.TimeUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Helpers {
    public static String formatDate(Date date) {
        DateFormat df =  new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        return df.format(date);
    }

    public static String skipConflictingTimeSlots(HashMap<String, RoomRecord> previousTimeSlots, String[] newTimeSlots, int roomNumber, String date, String RecordID) {
        StringBuilder timeConflicts = new StringBuilder();
        boolean skipTimeSlot;
        for (String newTime : newTimeSlots) {
            skipTimeSlot = false;
            for (String previousTime : previousTimeSlots.keySet()) {
                try {
                    if (!TimeUtils.validateTime(previousTime, newTime)) {
                        timeConflicts.append("Could not add " + newTime + ". Time conflict with " + previousTime + "\n");
                        skipTimeSlot = true;
                        break;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    return "ERROR PARSING DATE";
                }
            }
            if (skipTimeSlot) continue;

            previousTimeSlots.put(newTime, new RoomRecord(newTime, roomNumber, date, RecordID));
        }

        return timeConflicts.toString();
    }

    public static Date createDateFromString(String sDate) {
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        Date date = null;

        try {
            date = df.parse(sDate);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Failed to parse date!");
        }

        return date;
    }
}
