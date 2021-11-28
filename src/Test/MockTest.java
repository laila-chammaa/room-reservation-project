package Test;

import Replicas.Replica3.campus.CampusImpl;
import Replicas.Replica3.repo.CentralRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MockTest {
    @BeforeAll
    public static void setup() {
        new CampusImpl("WST").run();
        new CampusImpl("DVL").run();
        new CampusImpl("KKL").run();

        // Wait for all threads to startup
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void replica3AuthTest() {
        String res = MockUdpMessage.udpTest(CentralRepository.getUdpPortNum("KKL"));
        Assertions.assertEquals("Failure: failed to authenticate admin", res, "Output not as expected!");
    }
}
