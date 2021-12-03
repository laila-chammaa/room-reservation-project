package DRRS;

import Replicas.Replica1.com.Replica1;
import Replicas.Replica2.Replica2;
import Replicas.Replica3.campus.Replica3;
import Replicas.Replica4.com.Replica4;

import java.io.IOException;

public class ReplicaManagerDriverCrash {
    public static void main(String[] args) throws IOException {
        Replica replica1 = new Replica1();
        Replica replica2 = new Replica2();
        Replica replica3 = new Replica3(true);
        Replica replica4 = new Replica4();

        ReplicaManager rm1 = new ReplicaManager(1, replica1);
        ReplicaManager rm2 = new ReplicaManager(2, replica2);
        ReplicaManager rm3 = new ReplicaManager(3, replica3);
        ReplicaManager rm4 = new ReplicaManager(4, replica4);

        rm1.start();
        rm2.start();
        rm3.start();
        rm4.start();
    }
}
