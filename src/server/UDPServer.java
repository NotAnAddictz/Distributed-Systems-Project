package server;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import common.Marshaller;

public class UDPServer {
    public static void main(String[] args) {
        DatagramSocket serverSocket = null;
        Marshaller marshaller = new Marshaller();
        try {
            // Create a UDP socket
            serverSocket = new DatagramSocket(Integer.parseInt(args[0])); // Port number can be any available port

            byte[] receiveData = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                byte[] returnedMessage;
                DatagramPacket sendPacket;

                // Receive packet from client
                serverSocket.receive(receivePacket);
                System.out.println("Packet Received from: " + receivePacket.getAddress() + " Client Port: " + receivePacket.getPort());

                // Get client's address and port number
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                byte[] receivedMessage = receivePacket.getData();
                String[] unmarshalledStrings = marshaller.unmarshal(receivedMessage);
                
                int clientChosen = Integer.parseInt(unmarshalledStrings[0]);
                
                switch (clientChosen) {
                    case 1:
                        String fileContent = readFile(unmarshalledStrings);
                        returnedMessage = marshaller.marshal(1, fileContent);
                        TimeUnit.SECONDS.sleep(5);
                        sendPacket = new DatagramPacket(returnedMessage, returnedMessage.length, clientAddress, clientPort);
                        serverSocket.send(sendPacket);
                        break;
                
                    default:
                        break;
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

    public static String readFile(String[] unmarshalledStrings){
        String filePath = "src/resources/" + unmarshalledStrings[1];
        int offset = Integer.valueOf(unmarshalledStrings[2]);
        int noOfBytes = Integer.valueOf(unmarshalledStrings[3].trim()); // Number of bytes to read
        String content = "No content in file";
        
        try (
            // Read file content
            RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            file.seek(offset);
            byte[] buffer = new byte[noOfBytes];
            file.readFully(buffer);
            file.close();

            // Convert the byte array to a String
            String readValue = new String(buffer, StandardCharsets.UTF_8);
            return readValue;
        } catch (FileNotFoundException e) {
            content = "File does not exist on server";
        } catch (EOFException e) {
            content = "Offset exceed file length";
        } catch (IOException e) {
            content = "IOException Error";
            e.printStackTrace();
        }
            return content;
    }
}