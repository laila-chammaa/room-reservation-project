package DRRS;

import java.util.HashMap;
import java.util.Map;

public class Config {

    public static class Ports {

        public static final int REPLICA_PORT_1 = 4001;
        public static final int REPLICA_PORT_2 = 4002;
        public static final int REPLICA_PORT_3 = 4003;
        public static final int REPLICA_PORT_4 = 4004;
        
        private static final ReplicaPorts ReplicaManager1 = new ReplicaPorts(REPLICA_PORT_1, 5001, 5002, 5003, IPAddresses.REPLICA1);
        private static final ReplicaPorts ReplicaManager2 = new ReplicaPorts(REPLICA_PORT_2, 5004, 5005, 5006, IPAddresses.REPLICA2);
        private static final ReplicaPorts ReplicaManager3 = new ReplicaPorts(REPLICA_PORT_3, 5007, 5008, 5009, IPAddresses.REPLICA3);
        private static final ReplicaPorts ReplicaManager4 = new ReplicaPorts(REPLICA_PORT_4, 5010, 5011, 5012, IPAddresses.REPLICA4);
        
        public static final Map<Integer, ReplicaPorts> REPLICA_MANAGER_PORTS_MAP = new HashMap<Integer, ReplicaPorts>() {{
            put(1, ReplicaManager1); put(2, ReplicaManager2); put(3, ReplicaManager3); put(4, ReplicaManager4);
        }};
    }

    public static class PortNumbers
    {
        public static final int FE_SEQ = 9000; // From FE to Sequencer
        public static final int SEQ_RE = 9001; // From Sequencer to Replica (Multicast)
        public static final int RE_FE = 9002; // From Replica to FE
        public static final int SEQ_FE = 9003; // From Sequencer to FE
    }

    public static class IPAddresses
    {
        public static final String REPLICA1 = "132.205.64.255";
        public static final String REPLICA2 = "132.205.64.142";
        public static final String REPLICA3 = "132.205.64.143";
        public static final String REPLICA4 = "132.205.64.144";
        public static final String SEQUENCER = "132.205.64.255";
        public static final String FRONT_END = "132.205.64.142";
        public static final String MULTICAST_ADR = "239.1.2.3";
    }

    public enum StatusCode {
        SUCCESS,
        FAIL,
    }

    public enum Failure {
        NONE,
        BYZANTINE,
        PROCESS_CRASH,
    }

    public static final int MESSAGE_DELAY = 3;
    public static final int DELIVERY_QUEUE_MAX_SIZE = 1000;

    public static final String CREATE_ROOM = "create_record", CANCEL_BOOKING = "cancel_booking",
            GET_TIMESLOTS = "get_timeslots", DELETE_ROOM = "delete_room", CHANGE_RESERVATION = "change_reservation",
            BOOK_ROOM = "book_room", EXIT = "exit", ACK = "ack", RESTART_REPLICA = "restart_replica",
            BAD_REPLICA_NUMBER = "bad_replica_number", FAILED_REPLICA_RESTART_FAILED = "failed_replica_restart_failed",
            FAILED_REPLICA_RESTARTED = "failed_replica_restarted", GET_DATA = "get_data", SET_DATA = "set_data", REPORT_FAILURE = "report_failure",
            REPLICA_RESTARTED = "replica_restarted", FAILURE_COUNTS_INCREMENTED = "failure_counts_incremented";
}
