package common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class NetworkClient {
    private Marshaller marshaller;
    private InetAddress serverAddress;
    private int serverPort;
    private int packetId;
    private DatagramSocket socket;
    private DatagramPacket sendPacket; 

    public NetworkClient(DatagramSocket socket, String serverName, int serverPort) throws UnknownHostException {
        this.socket = socket;
        this.serverAddress = InetAddress.getByName(serverName);
        this.serverPort = serverPort;
        this.packetId = 0;
        this.marshaller = new Marshaller();
    }

    public void send(int funcId, String... args) {
        packetId += 1;
        byte[] sendData = marshaller.marshal(funcId, packetId, args);
        // Create packet to send to server
        sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);

        try {
            // Send packet to server
            socket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    public DatagramPacket receive() {
        int retries = 5;
        while (retries > 0) {
            try {
                while (true) {
                    byte[] receiveData = new byte[1024];

                    // Receive response from server
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    if (marshaller.unmarshal(sendPacket.getData())[1].equals(marshaller.unmarshal(receivePacket.getData())[1])) {
                        return receivePacket;
                    }
                }
            } catch (SocketTimeoutException e) {
                try {
                    if (retries > 1) {
                        socket.send(sendPacket);
                        retries -= 1;
                    } else {
                        return null;
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
