package client;

import java.net.*;
import java.util.Scanner;

import common.Marshaller;

public class UDPClient {
    DatagramSocket clientSocket;
    Scanner scanner;
    Marshaller marshaller;
    public static void main(String[] args) {
        String serverHostname = args[0];
        int serverPort = Integer.parseInt(args[1]);

        UDPClient udpClient = new UDPClient();
        udpClient.startProgram(serverHostname, serverPort);
    }
    
    public void startProgram(String serverHostname, int serverPort){
        scanner = new Scanner(System.in);
        clientSocket = null;
        marshaller = new Marshaller();

        try {
            // Create a UDP socket
            clientSocket = new DatagramSocket();

            // Get server's address
            InetAddress serverAddress = InetAddress.getByName(serverHostname);

            int chosen = 0;
            while (chosen != 5) {

                System.out.println("1: Read file on (n) bytes.");
                System.out.println("5: Exit program.");
                System.out.print("Enter option: ");
                chosen = Integer.parseInt(scanner.nextLine());
                
                switch(chosen) {
                    case 1:
                        readFile(serverAddress, serverPort);
                        break;
                    case 5:
                        System.out.println("Exiting program.");
                        break;
                    default:
                        System.out.println("Invalid option.");
                        break;
                }
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

    public void readFile(InetAddress serverAddress, int serverPort){
        try {
            System.out.print("Enter file name: ");
            String fileName = scanner.nextLine();
            System.out.print("Enter offset(bytes): ");
            int offset = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter number of bytes to read: ");
            int readBytes = Integer.parseInt(scanner.nextLine());

            byte[] sendData = marshaller.readFileMarshal(1, fileName, offset, readBytes);

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
            System.out.println("Server Replied: " + responseMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
