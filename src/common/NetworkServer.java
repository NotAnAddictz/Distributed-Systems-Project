package common;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import server.UDPServer.Callback;

public class NetworkServer {
    private Marshaller marshaller;
    private Dictionary<String, List<Callback>> sendList = new Hashtable<>(); // List to accept out
    private Dictionary<String, byte[]> messageList = new Hashtable<>(); // List to hold all the message contents to send
    private DatagramSocket socket;
    private int packetIndex = 0;

    public NetworkServer(DatagramSocket socket) {
        this.marshaller = new Marshaller();
        this.socket = socket;
    }

    public void send(Callback client, int funcId, String... args) {
        int packetId = packetIndex;
        packetIndex++;
        InetAddress clientAddress = client.getAdd();
        int clientPort = client.getPort();
        byte[] sendData = marshaller.marshal(funcId, packetId, args);
        // Create packet to send to server
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);

        try {
            // Send packet to server
            socket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reply(DatagramPacket receivedPacket, int funcId, String... args) {
        int packetId = Integer.parseInt(marshaller.unmarshal(receivedPacket.getData())[1]);
        InetAddress clientAddress = receivedPacket.getAddress();
        int clientPort = receivedPacket.getPort();
        byte[] sendData = marshaller.marshal(funcId, packetId, args);
        // Create packet to send to server
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);

        try {
            // Send packet to server
            socket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMass(String fileName, int retries) {
        // Sending to all clients
        while (retries < 5) {
            List<Callback> clientList = sendList.get(fileName);
            if (clientList == null || clientList.isEmpty()) {
                break;
            }
            byte[] sendData = messageList.get(fileName);
            for (Callback calls : clientList) {
                InetAddress address = calls.getAdd();
                int port = calls.getPort();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address,
                        port);
                try {
                    socket.send(sendPacket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // Waiting for 3 seconds, before retrying
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retries++;
        }
    }

    // Adds an update to the system
    public void newUpdate(byte[] sendData, String fileName, List<Callback> clients) {
        // Add / replace the message with the new message
        messageList.put(fileName, sendData);
        sendList.put(fileName, clients);
        // Creates a new thread to execute sends and handle retries
        new Thread() {
            public void run() {
                sendMass(fileName, 0);
            }
        }.start();
    }

    public void removeClient(String fileName, Callback client) {
        List<Callback> clientList = sendList.get(fileName);
        for (int i = 0; i < clientList.size(); i++) {
            Callback testClient = clientList.get(i);
            if (client.getAdd().equals(testClient.getAdd()) && testClient.getPort() == client.getPort()) {
                clientList.remove(testClient);
            }
        }
        sendList.put(fileName, clientList);
    }

}
