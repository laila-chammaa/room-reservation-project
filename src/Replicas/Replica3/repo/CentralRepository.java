package Replicas.Replica3.repo;

import Replicas.Replica3.campus.RoomRecord;

import java.util.HashMap;

public class CentralRepository {
    static int RRIDCount = 10000;
    static HashMap<String, RoomRecord> bookingRecord = new HashMap<>();

    public static void incrementRRIDCount() {
        CentralRepository.RRIDCount += 1;
    }

    public static int getRRIDCount() {
        return CentralRepository.RRIDCount;
    }

    public static HashMap<String, RoomRecord> getBookingRecord() {
        return CentralRepository.bookingRecord;
    }

}
