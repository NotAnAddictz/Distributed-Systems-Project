package client;

import java.net.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

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

    public void startProgram(String serverHostname, int serverPort) {
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
                System.out.println("2: Write file on (n) bytes.");
                System.out.println("3: Monitor updates on file.");
                System.out.println("5: Exit program.");
                System.out.print("Enter option: ");
                chosen = Integer.parseInt(scanner.nextLine());

                switch (chosen) {
                    case 1:
                        readFile(serverAddress, serverPort);
                        break;
                    case 2:
                        break;
                    case 3:
                        // Creating callback object
                        Callback callbackObject = new CallbackObject();
                        monitorUpdates(serverAddress, serverPort);
                        break;
                    case 4:
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

    public void readFile(InetAddress serverAddress, int serverPort) {
        try {
            int offset;
            int readBytes;
            System.out.printf("Enter file path: ");
            String filePathString = scanner.nextLine();
            while (true) {
                System.out.printf("Enter offset(bytes): ");
                String offsetString = scanner.nextLine();
                try {
                    offset = Integer.parseInt(offsetString);
                    if (offset < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Offset must be a positive integer");
                    continue;
                }
                break;
            }
            while (true) {
                System.out.printf("Enter number of bytes to read: ");
                String byteString = scanner.nextLine();
                try {
                    readBytes = Integer.parseInt(byteString);
                    if (readBytes < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Number of bytes must be a positive integer");
                    continue;
                }
                break;
            }
            byte[] sendData = marshaller.readFileMarshal(1, filePathString, offset, readBytes);

            // Create packet to send to server
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);

            // Send packet to server
            clientSocket.send(sendPacket);

            byte[] receiveData = new byte[1024];

            // Receive response from server
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            String[] unmarshalledStrings = marshaller.unmarshal(receivePacket.getData());
            int serverChosen = Integer.parseInt(unmarshalledStrings[0]);

            switch (serverChosen) {
                case 1:
                    // Print response from server
                    System.out.println("Server Replied: " + unmarshalledStrings[1]);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void monitorUpdates(InetAddress serverAddress, int serverPort) {
        try {
            System.out.println("Select File to monitor");
            String filePath = scanner.nextLine();
            System.out.println("Select Duration to monitor");
            int duration = scanner.nextInt();
            System.out.println("Scanning for updates on " + filePath + " for " + duration + " seconds");
            byte[] sendData = marshaller.monitorFileMarshal(3, filePath, duration);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);

            clientSocket.send(sendPacket);
            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                clientSocket.receive(receivePacket);

                String[] unmarshalledStrings = marshaller.unmarshal(receivePacket.getData());
                if (unmarshalledStrings[2] == "End") {
                    break;
                } else {
                    System.out.println(unmarshalledStrings[2]);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface Callback extends Remote {
        void callbackMethod() throws RemoteException;
    };

    public class CallbackObject implements Callback {
        @Override
        public void callbackMethod() throws RemoteException {
            System.out.println("File updated");
        }

        String filePath;

    }
}
