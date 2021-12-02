package Replicas.Replica1.com;

import DRRS.Config;
import DRRS.Replica;
import DRRS.ReplicaPorts;
import Replicas.Replica3.campus.CampusImpl;

import java.util.HashMap;

public class Replica1 extends Replica {

    private static final ReplicaPorts ports = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(1);
    
    public Replica1() {
        super(
                new CampusServer("DVL", ports.getDvlPort(),
                        new HashMap<String, String>() {{
                            put("DVL", "localhost:" + ports.getDvlPort());
                            put("KKL", "localhost:" + ports.getKklPort());
                            put("WST", "localhost:" + ports.getWstPort());
                        }}),
                new CampusServer("KKL", ports.getDvlPort(),
                        new HashMap<String, String>() {{
                            put("DVL", "localhost:" + ports.getDvlPort());
                            put("KKL", "localhost:" + ports.getKklPort());
                            put("WST", "localhost:" + ports.getWstPort());
                        }}),
                new CampusServer("WST", ports.getDvlPort(),
                        new HashMap<String, String>() {{
                            put("DVL", "localhost:" + ports.getDvlPort());
                            put("KKL", "localhost:" + ports.getKklPort());
                            put("WST", "localhost:" + ports.getWstPort());
                        }})
        );
        dvlThread = new Thread((CampusServer) dvlCampus);
        kklThread = new Thread((CampusServer) kklCampus);
        wstThread = new Thread((CampusServer) wstCampus);
    }
}
