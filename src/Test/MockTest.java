package Test;

import Replicas.Replica3.campus.CampusImpl;
import Replicas.Replica3.repo.CentralRepository;
import Replicas.UDPUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MockTest {
    @BeforeAll
    public static void setup() {
        new CampusImpl("WSTRepo3").run();
        new CampusImpl("DVLRepo3").run();
        new CampusImpl("KKLRepo3").run();

        // Wait for all threads to startup
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void replica3AuthTest() {
        String res = MockUdpMessage.udpTest(UDPUtils.getUdpPortNum("KKLRepo3"));
        Assertions.assertEquals("Failure: failed to authenticate admin", res, "Output not as expected!");
    }
}
