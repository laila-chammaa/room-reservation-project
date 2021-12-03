package Replicas.Replica4.udp;


import Replicas.Replica4.com.CampusServer;

import java.io.Serializable;

public interface CampusUDPInterface extends Serializable {
    void execute(CampusServer campusServer, String campusID);
}
