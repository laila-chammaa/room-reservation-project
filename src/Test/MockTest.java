package Test;

import DRRS.Replica;
import Replicas.Replica1.com.Replica1;
import Replicas.Replica2.Replica2;
import Replicas.Replica3.campus.CampusImpl;
import Replicas.Replica3.campus.Replica3;
import Replicas.Replica3.repo.CentralRepository;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class MockTest {
    @BeforeAll
    public static void setup() {

        Replica rep3 = new Replica3();
        rep3.startServers();

        // Wait for all threads to startup
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        JSONObject obj = rep3.getCurrentData();

        rep3.setCurrentData(obj);
        JSONObject obj2 = rep3.getCurrentData();

    }

    @Test
    public void replica3AuthTest() {
//        String res = MockUdpMessage.udpTest(UDPUtils.getUdpPortNum("KKLRepo3"));
//        Assertions.assertEquals("Failure: failed to authenticate admin", res, "Output not as expected!");
    }
}
