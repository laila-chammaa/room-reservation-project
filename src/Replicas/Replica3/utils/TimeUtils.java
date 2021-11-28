package Replicas.Replica3.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtils {
    public static boolean validateTime(String previousTimeSlot, String newTimeSlot) throws ParseException {
        String startTime = previousTimeSlot.split("-")[0];
        Date time = new SimpleDateFormat("HH:mm").parse(startTime);
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);

        String endTime = previousTimeSlot.split("-")[1];
        Date time2 = new SimpleDateFormat("HH:mm").parse(endTime);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(time2);

        String newStartTime = newTimeSlot.split("-")[0];
        Date time3 = new SimpleDateFormat("HH:mm").parse(newStartTime);
        Calendar cal3 = Calendar.getInstance();
        cal3.setTime(time3);

        String newEndTime = newTimeSlot.split("-")[1];
        Date time4 = new SimpleDateFormat("HH:mm").parse(newEndTime);
        Calendar cal4 = Calendar.getInstance();
        cal4.setTime(time4);

        Date start = cal3.getTime();
        Date end = cal4.getTime();

        if ( (start.after(cal.getTime()) && start.before(cal2.getTime()))
                || (end.after(cal.getTime()) && end.before(cal2.getTime()))) {
            return false;
        }

        return true;
    }

    public static boolean validateEndTime(String timeSlot) throws ParseException {
        String startTime = timeSlot.split("-")[0];
        String endTime = timeSlot.split("-")[1];

        Date time = new SimpleDateFormat("HH:mm").parse(startTime);
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);

        Date time2 = new SimpleDateFormat("HH:mm").parse(endTime);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(time2);

        Date end = cal2.getTime();
        if (end.before(cal.getTime())) {
            return false;
        }

        return true;
    }
}
