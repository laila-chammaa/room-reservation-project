package Replicas.Replica3.campus;

import DRRS.Config;
import DRRS.Replica;
import DRRS.ReplicaPorts;

import java.util.HashMap;

public class Replica3 extends Replica {

    private static final ReplicaPorts ports = Config.Ports.REPLICA_MANAGER_PORTS_MAP.get(3);

    public Replica3() {
        super(
                new CampusImpl("DVL", ports.getDvlPort(), new HashMap<String, Integer>(){{ put("DVL", ports.getDvlPort()); put("KKL", ports.getKklPort()); put("WST", ports.getWstPort()); }}),
                new CampusImpl("KKL", ports.getKklPort(), new HashMap<String, Integer>(){{ put("DVL", ports.getDvlPort()); put("KKL", ports.getKklPort()); put("WST", ports.getWstPort()); }}),
                new CampusImpl("WST", ports.getWstPort(), new HashMap<String, Integer>(){{ put("DVL", ports.getDvlPort()); put("KKL", ports.getKklPort()); put("WST", ports.getWstPort()); }})
        );
    }

    public Replica3(boolean inducedCrash, boolean inducedByzantineFailure) {
        super(
                new CampusImpl("DVL", ports.getDvlPort(), new HashMap<String, Integer>(){{ put("DVL", ports.getDvlPort()); put("KKL", ports.getKklPort()); put("WST", ports.getWstPort()); }}, inducedCrash, inducedByzantineFailure),
                new CampusImpl("KKL", ports.getKklPort(), new HashMap<String, Integer>(){{ put("DVL", ports.getDvlPort()); put("KKL", ports.getKklPort()); put("WST", ports.getWstPort()); }}, inducedCrash, inducedByzantineFailure),
                new CampusImpl("WST", ports.getWstPort(), new HashMap<String, Integer>(){{ put("DVL", ports.getDvlPort()); put("KKL", ports.getKklPort()); put("WST", ports.getWstPort()); }}, inducedCrash, inducedByzantineFailure)
        );
    }
}
