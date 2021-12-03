package Replicas.Replica4.com;

import DRRS.Config;
import DRRS.Replica;
import DRRS.ReplicaPorts;

import java.util.HashMap;

public class Replica4 extends Replica {

    private static final ReplicaPorts ports = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(4);
    private static final String host = Config.IPAddresses.REPLICA4 + ":";
    
    public Replica4() {
        super(
                new CampusServer("DVL", ports.getDvlPort(),
                        new HashMap<String, String>() {{
                            put("DVL", host + ports.getDvlPort());
                            put("KKL", host + ports.getKklPort());
                            put("WST", host + ports.getWstPort());
                        }}),
                new CampusServer("KKL", ports.getKklPort(),
                        new HashMap<String, String>() {{
                            put("DVL", host + ports.getDvlPort());
                            put("KKL", host + ports.getKklPort());
                            put("WST", host + ports.getWstPort());
                        }}),
                new CampusServer("WST", ports.getWstPort(),
                        new HashMap<String, String>() {{
                            put("DVL", host + ports.getDvlPort());
                            put("KKL", host + ports.getKklPort());
                            put("WST", host + ports.getWstPort());
                        }})
        );
    }
}
