package Replicas.Replica3.campus;

import java.io.Serializable;

public interface UdpMessageInterface  extends Serializable {
    String getOpName();
    String getID();
    String getCampusName();
    String getRoomNumber();
    String getTimeSlot();
    String[] getTimeSlots();
    String getDate();
    String getBookingID();
}
