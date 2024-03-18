package client;

import java.net.*;
import java.util.Scanner;

public class UDPClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String serverHostname = args[0];
        int serverPort = Integer.parseInt(args[1]);

        DatagramSocket clientSocket = null;
        try {
            // Create a UDP socket
            clientSocket = new DatagramSocket();

            // Get server's address
            InetAddress serverAddress = InetAddress.getByName(serverHostname);

            while (true) {

                System.out.print("1:Read first 5 char of test file\nEnter option: ");
                String message = scanner.nextLine();
                if(Integer.parseInt(message) <= 0){
                    throw new Exception("Invalid Option");
                }
                byte[] sendData = message.getBytes();

                // Create packet to send to server
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);

                // Send packet to server
                clientSocket.send(sendPacket);

                byte[] receiveData = new byte[1024];

                // Receive response from server
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);

                // Print response from server
                String responseMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Received from server: " + responseMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (clientSocket != null) {
                clientSocket.close();
            }
            scanner.close();
        }
    }
}
