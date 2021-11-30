package Replicas.Replica1.udp;


import Replicas.Replica1.com.CampusServer;

import java.io.Serializable;

public interface CampusUDPInterface extends Serializable {
    void execute(CampusServer campusServer, String campusID);
}
