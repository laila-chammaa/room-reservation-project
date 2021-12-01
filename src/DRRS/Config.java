package DRRS;

public class Config {

    public static class Replica1 {
        public static final int RM_PORT = 9000;
        public static final int RE_PORT = 9100;
        public static final int DVL_PORT = 5000;
        public static final int KKL_PORT = 5001;
        public static final int WST_PORT = 5002;
    }

    public static class Replica2 {
        public static final int RM_PORT = 9001;
        public static final int RE_PORT = 9101;
        public static final int DVL_PORT = 6000;
        public static final int KKL_PORT = 6001;
        public static final int WST_PORT = 6002;
    }

    public static class Replica3 {
        public static final int RM_PORT = 9002;
        public static final int RE_PORT = 9102;
        public static final int DVL_PORT = 7000;
        public static final int KKL_PORT = 7001;
        public static final int WST_PORT = 7002;
    }

    public static class Replica4 {
        public static final int RM_PORT = 9003;
        public static final int RE_PORT = 9103;
        public static final int DVL_PORT = 8000;
        public static final int KKL_PORT = 8001;
        public static final int WST_PORT = 8002;
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

    public static final String CREATE_ROOM = "create_record", CANCEL_BOOKING = "cancel_booking",
            GET_TIMESLOTS = "get_timeslots", DELETE_ROOM = "delete_room", CHANGE_RESERVATION = "change_reservation",
            BOOK_ROOM = "book_room", EXIT = "exit", ACK = "ack", RESTART_REPLICA = "restart_replica",
            BAD_REPLICA_NUMBER = "bad_replica_number", FAILED_REPLICA_RESTART_FAILED = "failed_replica_restart_failed",
            FAILED_REPLICA_RESTARTED = "failed_replica_restarted", GET_DATA = "get_data", SET_DATA = "set_data", REPORT_FAILURE = "report_failure",
            REPLICA_RESTARTED = "replica_restarted", FAILURE_COUNTS_INCREMENTED = "failure_counts_incremented";
}
