package Client;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ClientLogUtil {
    static Logger initiateLogger(String campusID, String userID) throws RemoteException {
        String filename;
        char userType = userID.charAt(StudentClient.USER_TYPE_POS);

        if (userType == 'S') {
            filename = "Clients Logs/" + campusID + " - " + userID;
        } else if (userType == 'A') {
            filename = "Clients Logs/" + campusID + " - " + userID;
        } else {
            throw new RemoteException("Error: Invalid User ID.");
        }

        Logger logger = Logger.getLogger(filename);
        FileHandler fh;

        try {
            fh = new FileHandler(filename + ".log");

            //Disable console handling
            //logger.setUseParentHandlers(false);
            logger.addHandler(fh);

            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException e) {
            System.err.println("Clients Log: Error: Security Exception " + e);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Clients Log: Error: IO Exception " + e);
            e.printStackTrace();
        }

        System.out.println("Clients Log: Logger initialization success.");

        return logger;
    }
}
