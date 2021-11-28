package Replicas;

public class UDPUtils {
    public static int getUdpPortNum(String serverName) {
        switch (serverName) {
            case "DVLRepo3":
                return 8081;
            case "WSTRepo3":
                return 8082;
            case "KKLRepo3":
                return 8083;
            default:
                return 0;
        }
    }
}
