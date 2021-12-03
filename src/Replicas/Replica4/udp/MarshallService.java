package Replicas.Replica4.udp;

import java.io.*;

public class MarshallService implements Serializable {
    private static final long serialVersionUID = 1L;

    //We will translate the data object to byte array for UDP requests
    public static byte[] marshall(CampusUDPInterface data) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream output = new ObjectOutputStream(outputStream);

            output.writeObject(data);
            output.flush();
            output.close();

            return outputStream.toByteArray();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    //We will translate the byte array to CampusUDPInterface object from UDP requests
    public static CampusUDPInterface unmarshall(byte[] data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            ObjectInputStream input = new ObjectInputStream(inputStream);

            CampusUDPInterface dataUnmarshall = (CampusUDPInterface) input.readObject();
            inputStream.close();
            input.close();

            return dataUnmarshall;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}