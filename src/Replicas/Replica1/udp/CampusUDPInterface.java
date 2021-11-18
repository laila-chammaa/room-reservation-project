package Replicas.Replica1.udp;


import Replicas.Replica1.com.ServerInterface;
import Replicas.Replica1.model.CampusID;

import java.io.Serializable;

public interface CampusUDPInterface extends Serializable {
    void execute(ServerInterface campusServer, CampusID campusID);
}
