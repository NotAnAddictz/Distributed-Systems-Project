package client;

import java.net.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Scanner;

import common.Helper;
import common.Marshaller;

public class UDPClient {
    DatagramSocket clientSocket;
    Scanner scanner;
    Marshaller marshaller;
    InetAddress serverAddress;
    int serverPort;
    CacheManager cacheManager;

    public static void main(String[] args) {
        String serverHostname = args[0];
        int serverPort = Integer.parseInt(args[1]);
        long freshnessIntervalInSeconds = Long.parseLong(args[2]);
        
        UDPClient udpClient = new UDPClient();
        udpClient.startProgram(serverHostname, serverPort, freshnessIntervalInSeconds);
    }

    public void startProgram(String serverHostname, int serverPort, long freshnessIntervalInSeconds) {
        System.out.println(String.format("Starting client {Server Name: %s | Port: %s | FreshnessInterval: %s}", serverHostname, serverPort, freshnessIntervalInSeconds));
        scanner = new Scanner(System.in);
        clientSocket = null;
        marshaller = new Marshaller();
        cacheManager = new CacheManager(freshnessIntervalInSeconds);
        this.serverPort = serverPort;

        try {
            // Create a UDP socket
            clientSocket = new DatagramSocket();

            // Get server's address
            serverAddress = InetAddress.getByName(serverHostname);

            int chosen = 0;
            while (chosen != 5) {
                System.out.println("==================================");
                System.out.println("1: Read file on (n) bytes.");
                System.out.println("2: Write to file.");
                System.out.println("3: List all files.");
                System.out.println("4: Monitor File Updates");
                System.out.println("5: Exit program.");
                System.out.println("==================================");
                System.out.print("Enter option: ");

                chosen = Integer.parseInt(scanner.nextLine());
                System.out.println("");
                boolean monitoring = false;
                Boolean readFromFile;
                switch (chosen) {
                    case 1:
                        readFromFile = readFile();
                        if (readFromFile) {
                            //only require receive() when getting data from server.
                            //skip this when cache has required data.
                            receive();
                        }
                        break;
                    case 2:
                        writeToFile();
                        receive();
                        break;
                    case 3:
                        listAllFiles();
                        receive();
                        break;
                    case 4:
                        monitorUpdates();
                        monitoring = true;
                        if (!monitoring) {
                            receive();
                            monitoring = false;
                        }
                        break;
                    case 5:
                        System.out.println("Exiting program.");
                        System.exit(200);
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

    public void receive() {
        try {
            byte[] receiveData = new byte[1024];

            // Receive response from server
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            String[] unmarshalledStrings = marshaller.unmarshal(receivePacket.getData());
            int serverChosen = Integer.parseInt(unmarshalledStrings[0]);
            switch (serverChosen) {
                case 1:
                    // Print response from server for READ FILE
                    String data = unmarshalledStrings[4];
                    if (data.startsWith("404:")) {
                        data = data.substring(4);
                    } else {
                        System.out.println("CLIENT RECEIVED: " + Arrays.toString(unmarshalledStrings));
                        cacheManager.addToCache(unmarshalledStrings[1], Integer.parseInt(unmarshalledStrings[2]), data, Long.parseLong(unmarshalledStrings[5]));
                    }
                    System.out.println("Server Replied: " + data);
                    break;
                case 2:
                    System.out.println("List of files: ");
                    System.out.println(unmarshalledStrings[1]);
                    break;
                case 4:
                    // Print response from server for WRITE TO FILE
                    System.out.println("Server Replied: " + unmarshalledStrings[1] + " | Updated " + unmarshalledStrings[2] + " at " + Helper.convertLastModifiedTime(Long.parseLong(unmarshalledStrings[3])));
                    cacheManager.setLastModified(unmarshalledStrings[2], Long.parseLong(unmarshalledStrings[3]));
                    break;
                default:
                    System.out
                            .println("Received an invalid funcID from " + receivePacket.getSocketAddress().toString());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean readFile() {
        try {
            int offset;
            int readBytes;
            listAllFiles();
            receive();
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
            if (!cacheManager.fileExistInCache(filePathString) || cacheManager.readFromCache(filePathString, offset, readBytes) == null) {
                // File does not exist in cache, retrieve from server
                // lastModifiedTime is updated in receive()
                byte[] sendData = marshaller.readFileMarshal(1, filePathString, offset, readBytes);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
                clientSocket.send(sendPacket);
                System.out.println("FILE NOT IN CACHE");
                return true;
            }
            if (!cacheManager.isValidated(filePathString)) {
                // File exists in cache but not validated
                //get lastModifiedTime from server
                //check for lastModified of file from server
                byte[] sendData = marshaller.marshal(10, filePathString);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
                clientSocket.send(sendPacket);
                
                byte[] receiveData = new byte[1024];
                // Receive response from server
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);

                String[] unmarshalledStrings = marshaller.unmarshal(receivePacket.getData());
                Long lastModifiedTime = Long.parseLong(unmarshalledStrings[1]);
                System.out.println("FILE IS NOT FRESH");

                if (cacheManager.isModified(filePathString, lastModifiedTime)) {
                    // File is outdated, get file from server
                    // lastModifiedTime is updated in receive()
                    System.out.println("FILE IS NOT UPDATED");
                    sendData = marshaller.readFileMarshal(1, filePathString, offset, readBytes);
                    sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
                    clientSocket.send(sendPacket);
                    return true;
                } else {
                    cacheManager.setValidated(filePathString);
                }
            }
            
            // File exists in cache and is validated or not modified
            // Read from cache
            System.out.println("FILE IS UPDATED IN CACHE");
            String data = cacheManager.readFromCache(filePathString, offset, readBytes);
            System.out.println("Data found in cache: " + data);
            return false;

            // if (cacheManager.fileExistInCache(filePathString)) {
            //     if (!cacheManager.isValidated(filePathString)) {
            //         //get lastModifiedTime from server
            //         Long lastModifiedTime = 1L;
            //         if (cacheManager.isModified(filePathString, lastModifiedTime)) {
            //             //file is outdated, get file from server. return True 
            //             //repeat code as file does not exist in cache.
            //             //lastModifiedTiem is updated in receive()
            //         } else {
            //             //file is not outdated so retrieve from cache and update cache lastModifiedTime.
            //             //readFromCache and Update manually lastModifiedTime here
            //             //return False
            //         }
            //     } else {
            //         //file in cache is still validate so retrieve from cache
            //         //readFromCache and Update manually lastModifiedTime here
            //         //return False
            //     }
            // } else {
            //     //get from server, file does not exist in cache.
            //     //lastModifiedTime is updated in receive();
            //     //return True
            // }

            // String data = cacheManager.readFromCache(filePathString, offset, readBytes);
            // if (data != null) {
            //     //Data in cache
            //     System.out.println("Data found in cache: " + data);
            //     return false;
            // } else {
            //     //check for lastModified of file from server
            //     byte[] sendData = marshaller.marshal(10, filePathString);
            //     DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            //     clientSocket.send(sendPacket);
                
            //     byte[] receiveData = new byte[1024];
            //     // Receive response from server
            //     DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            //     clientSocket.receive(receivePacket);

            //     String[] unmarshalledStrings = marshaller.unmarshal(receivePacket.getData());
            //     Long lastModifiedTime = Long.parseLong(unmarshalledStrings[1]);
            //     if (cacheManager.isModified(data, lastModifiedTime)) {
            //         //Data not cached
            //         sendData = marshaller.readFileMarshal(1, filePathString, offset, readBytes);

            //         // Create packet to send to server
            //         sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);

            //         // Send packet to server
            //         clientSocket.send(sendPacket);
            //         return true;
            //     } else {
            //         //Data not modified
            //         //read from cache again?
            //     }
            //}
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void writeToFile() {
        try {
            int offset;
            listAllFiles();
            receive();
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
            System.out.printf("Write: ");
            String writeString = scanner.nextLine();
            byte[] sendData = marshaller.writeFileMarshal(2, filePathString, offset, writeString);
            cacheManager.addToCache(filePathString, offset, writeString, null);
            // Create packet to send to server
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);

            // Send packet to server
            clientSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listAllFiles() {
        try {
            byte[] sendData = marshaller.marshal(3);

            // Create packet to send to server
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);

            // Send packet to server
            clientSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void monitorUpdates() {
        try {
            int duration = 0;
            System.out.printf("Select File to monitor: ");
            String filePath = scanner.nextLine();
            while (true) {
                System.out.printf("Select Duration to monitor: ");
                String durationString = scanner.nextLine();
                try {
                    duration = Integer.parseInt(durationString);
                    if (duration < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    System.out.printf("duration must be a positive integer");
                    continue;
                }
                break;
            }
            System.out.println("Scanning for updates on " + filePath + " for " + duration + " seconds");
            byte[] sendData = marshaller.monitorFileMarshal(4, filePath, duration);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);

            clientSocket.send(sendPacket);
            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                clientSocket.receive(receivePacket);

                String[] unmarshalledStrings = marshaller.unmarshal(receivePacket.getData());
                int message = Integer.parseInt(unmarshalledStrings[1]);
                if (message == 1) {
                    System.out.println("Ending Monitoring");
                    break;
                } else {
                    System.out.println("Monitoring received with funcId: " + unmarshalledStrings[0] + " | lastModifiedTime: " + Helper.convertLastModifiedTime(Long.parseLong(unmarshalledStrings[4])));
                    System.out.println(unmarshalledStrings[2]);
                    cacheManager.clearAndReplaceCache(filePath, unmarshalledStrings[3], Long.parseLong(unmarshalledStrings[4]));
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
