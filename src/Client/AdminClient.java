package Client;

import Client.com.FrontendImplService;
import Client.com.FrontendInterface;
import Client.com.net.java.dev.jaxb.array.StringArray;

import java.util.logging.Logger;


public class AdminClient {

    private String adminID;
    private String campusID;
    private Logger logger;
    private FrontendInterface server;

    private static final int USER_TYPE_POS = 3;
    private static final int CAMPUS_NAME_POS = 3;

    public AdminClient(String userID) throws Exception {
        validateAdmin(userID);
        try {
            this.logger = ClientLogUtil.initiateLogger(campusID, userID);
        } catch (Exception e) {
            throw new Exception("Login Error: Invalid ID.");
        }

        FrontendImplService service = new FrontendImplService();
        server = service.getFrontendImplPort();

        System.out.println("Login Succeeded. | Admin ID: " +
                this.adminID + " | Campus ID: " + this.campusID);
    }

    private void validateAdmin(String userID) throws Exception {
        char userType = userID.charAt(USER_TYPE_POS);
        String campusName = userID.substring(0, CAMPUS_NAME_POS);

        if (userType != 'A') {
            throw new Exception("Login Error: This client is for admins only.");
        }
        this.adminID = userID;
        this.campusID = campusName;
    }

    public synchronized void createRoom(int roomNumber, String date, StringArray listOfTimeSlots) {
        this.logger.info(String.format("Client Log | Request: createRoom | AdminID: %s | Room number: %d | Date: %s",
                adminID, roomNumber, date));
        this.logger.info(server.createRoom(adminID, roomNumber, date, listOfTimeSlots));
    }

    public synchronized void deleteRoom(int roomNumber, String date, StringArray listOfTimeSlots) {
        this.logger.info(String.format("Client Log | Request: deleteRoom | AdminID: %s | Room number: %d | Date: %s",
                adminID, roomNumber, date));
        this.logger.info(server.deleteRoom(adminID, roomNumber, date, listOfTimeSlots));
    }
}
