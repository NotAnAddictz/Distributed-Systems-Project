package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import common.Marshaller;

public class UDPServer {
    public static void main(String[] args) {
        DatagramSocket serverSocket = null;
        Marshaller marshaller = new Marshaller();
        try {
            // Create a UDP socket
            serverSocket = new DatagramSocket(9877); // Port number can be any available port

            byte[] receiveData = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                byte[] returnedMessage;

                // Receive packet from client
                serverSocket.receive(receivePacket);

                // Get client's address and port number
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Received from client: " + receivedMessage);
                
                if(Integer.parseInt(receivedMessage) == 1){
                    String fileContent = readFile();
                    System.out.println("File content: " + fileContent);
                    returnedMessage = marshaller.marshal(fileContent);
                    DatagramPacket sendPacket = new DatagramPacket(returnedMessage, returnedMessage.length, clientAddress, clientPort);
                    serverSocket.send(sendPacket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }

    public static String readFile(){

        String filePath = "src/resources/testfile.txt";
        int n = 5; // Number of bytes to read
        String content = "No content in file";

        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[n];
            int bytesRead = fis.read(buffer);

            if (bytesRead != -1) {
                content = new String(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
}