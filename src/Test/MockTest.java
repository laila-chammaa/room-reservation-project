package Test;

import Replicas.Replica3.campus.CampusImpl;
import Replicas.Replica3.repo.CentralRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MockTest {
    @BeforeAll
    public static void setup() {
        new Thread(() -> new CampusImpl("WST")).start();
        new Thread(() -> new CampusImpl("DVL")).start();
        new Thread(() -> new CampusImpl("KKL")).start();

        // Wait for all threads to startup
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void replica3Mock() {
        String res = MockUdpMessage.udpTest(CentralRepository.getUdpPortNum("DVL"));
        Assertions.assertEquals("asdasf booked on campus dvl", res, "Output not as expected!");
    }
}
