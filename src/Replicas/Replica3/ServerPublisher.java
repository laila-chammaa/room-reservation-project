package Replicas.Replica3;

import Replicas.Replica3.campus.CampusImpl;

public class ServerPublisher {
    public static void main(String[] args) {
        new Thread(() -> new CampusImpl("WST")).start();
        new Thread(() -> new CampusImpl("DVL")).start();
        new Thread(() -> new CampusImpl("KKL")).start();
    }
}
