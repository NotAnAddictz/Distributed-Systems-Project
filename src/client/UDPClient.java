package client;

import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.Time;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import common.Helper;
import common.Marshaller;
import common.NetworkClient;

public class UDPClient {
    Scanner scanner;
    Marshaller marshaller;
    InetAddress serverAddress;
    int serverPort;
    CacheManager cacheManager;
    NetworkClient network;

    public static void main(String[] args) {
        // Program arguments
        String serverHostname = args[0];
        int serverPort = Integer.parseInt(args[1]);
        long freshnessIntervalInSeconds = Long.parseLong(args[2]);
        UDPClient udpClient = new UDPClient();
        udpClient.startProgram(serverHostname, serverPort, freshnessIntervalInSeconds);
    }

    public void startProgram(String serverHostname, int serverPort, long freshnessIntervalInSeconds) {
        System.out.println(String.format("Starting client {Server Name: %s | Port: %s | FreshnessInterval: %s}",
                serverHostname, serverPort, freshnessIntervalInSeconds));
        scanner = new Scanner(System.in);
        marshaller = new Marshaller();
        cacheManager = new CacheManager(freshnessIntervalInSeconds);
        try {
            // Configuring client socket
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(3000);
            network = new NetworkClient(socket, serverHostname, serverPort);
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
            scanner.close();
            System.exit(0);
        }

        int chosen = 0;
        while (true) {
            try {
                cacheManager.printCacheContents();
                // UI
                System.out.println("==================================");
                System.out.println("1: Read file on (n) bytes.");
                System.out.println("2: Write to file.");
                System.out.println("3: List all files.");
                System.out.println("4: Monitor File Updates.");
                System.out.println("5: Delete file.");
                System.out.println("6: Insert file.");
                System.out.println("7: Exit program.");
                System.out.println("==================================");
                System.out.print("Enter option: ");

                // User input flow
                chosen = Integer.parseInt(scanner.nextLine());
                System.out.println("");
                switch (chosen) {
                    case 1:
                        // Task 1
                        readFile();
                        break;
                    case 2:
                        // Task 2
                        writeToFile();
                        break;
                    case 3:
                        // Idempotent Task 3
                        listAllFiles();
                        break;
                    case 4:
                        // Task 4
                        monitorUpdates();
                        break;
                    case 5:
                        // Idempotent Task 5
                        deleteFile();
                        break;
                    case 6:
                        // Non-Idempotent Task 6
                        insertFile();
                        break;
                    case 7:
                        System.out.println("Exiting program.");
                        System.exit(200);
                        break;
                    default:
                        System.out.println("Invalid option.");
                        break;
                }
            } catch (NumberFormatException n) {
                System.out.println("Please enter a valid number");
                continue;
            } catch (Exception e) {
                scanner.close();
                e.printStackTrace();
                break;
            }
        }
    }

    public boolean receive(DatagramPacket receivePacket) {
        // No packet received from server
        if (receivePacket == null) {
            return false;
        }
        try {
            // Read received packet
            String[] unmarshalledStrings = new Marshaller().unmarshal(receivePacket.getData());
            //System.out.println("RECEIVE(): " + Arrays.toString(unmarshalledStrings));
            int serverChosen = Integer.parseInt(unmarshalledStrings[0]);
            switch (serverChosen) {
                case 1:
                    // Print response from server for READ FILE
                    String data = unmarshalledStrings[5];
                    if (data.startsWith("404:")) {
                        data = data.substring(4);
                    } else {
                        System.out.println("CLIENT RECEIVED: " + Arrays.toString(unmarshalledStrings) + "CONVERTED TIME: " + Helper.convertLastModifiedTime(Long.parseLong(unmarshalledStrings[6])));
                        cacheManager.addToCache(unmarshalledStrings[2], Integer.parseInt(unmarshalledStrings[3]), data);
                        Boolean cacheContainsNewData = cacheManager.stringExists(unmarshalledStrings[2], data);
                        //should only setLastModified if file content is up to date with server. e.g. WRITTEN content above is the only content we are sure is up to date.
                        if (cacheContainsNewData) {
                            //above will be true if cache is entirely overwritten by client write.
                            cacheManager.setLastModified(unmarshalledStrings[2], Long.parseLong(unmarshalledStrings[6]));
                        }
                    }
                    System.out.println("Server Replied: " + data);
                    break;
                case 2:
                    // Display list of files
                    System.out.println("List of files: ");
                    System.out.println(unmarshalledStrings[2]);
                    break;
                case 3:
                    // Print simple string messages
                    System.out.println(unmarshalledStrings[2]);
                    break;
                case 4:
                    // Print response from server for WRITE TO FILE
                    Boolean cacheContainsNewData = cacheManager.stringExists(unmarshalledStrings[3], unmarshalledStrings[5]);
                    //should only setLastModified if file content is up to date with server. e.g. WRITTEN content above is the only content we are sure is up to date.
                    if (cacheContainsNewData) {
                        //above will be true if cache is entirely overwritten by client write.
                        System.out.println("Server Replied: " + unmarshalledStrings[2] + " | Updated " + unmarshalledStrings[3] + " and Updated LastModifiedTime to " + Helper.convertLastModifiedTime(Long.parseLong(unmarshalledStrings[4])));
                        cacheManager.setLastModified(unmarshalledStrings[3], Long.parseLong(unmarshalledStrings[4]));
                    } else {
                        System.out.println("Server Replied: " + unmarshalledStrings[2] + " | Updated " + unmarshalledStrings[3]);
                    }
                    break;
                case 5:
                    //delete
                    System.out.println("IN CASE 5: " + Arrays.toString(unmarshalledStrings));
                    System.out.println("Server Replied: " + unmarshalledStrings[2]);
                    if (cacheManager.fileExistInCache(unmarshalledStrings[3])) {
                        cacheManager.removeFromCache(unmarshalledStrings[3]);
                    }
                    break;
                case 6:
                    //insert
                    System.out.println("IN CASE 6: " + Arrays.toString(unmarshalledStrings));
                    System.out.println("Server Replied: " + unmarshalledStrings[2] + " added to server files");

                    cacheManager.clearAndReplaceCache(unmarshalledStrings[2], unmarshalledStrings[3], Long.parseLong(unmarshalledStrings[4]));
                    break;
                    
                default:
                    System.out
                            .println("Received an invalid funcID from " + receivePacket.getSocketAddress().toString());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // Retrieve txt file on the server and read
    public boolean readFile() {
        int offset;
        int readBytes;

        // Display all files
        if (listAllFiles()) {

            // If successfully displayed
            System.out.printf("Enter file path: ");
            String filePathString = scanner.nextLine();
            String offsetString;

            // Offset input
            while (true) {
                System.out.printf("Enter offset(bytes): ");
                offsetString = scanner.nextLine();
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

            // Number of bytes to read input
            String byteString;
            while (true) {
                System.out.printf("Enter number of bytes to read: ");
                byteString = scanner.nextLine();
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
            if (!cacheManager.fileExistInCache(filePathString)
                    || cacheManager.readFromCache(filePathString, offset, readBytes) == null) {
                // File does not exist in cache, retrieve from server
                // lastModifiedTime is updated in receive()
                network.send(1, filePathString, offsetString, byteString);
                System.out.println("FILE OR DATA NOT IN CACHE");
                receive(network.receive());
                return true;
            }
            if (!cacheManager.isValidated(filePathString)) {
                // File exists in cache but not validated
                // get lastModifiedTime from server
                // check for lastModified of file from server
                network.send(10, filePathString);
                // Receive response from server
                DatagramPacket receivePacket = network.receive();

                String[] unmarshalledStrings = marshaller.unmarshal(receivePacket.getData());
                Long lastModifiedTime = Long.parseLong(unmarshalledStrings[2]);
                System.out.println("FILE IS NOT FRESH");

                if (cacheManager.isModified(filePathString, lastModifiedTime)) {
                    // File is outdated, get file from server
                    // lastModifiedTime is updated in receive()
                    System.out.println("FILE IS NOT UPDATED");
                    network.send(1, filePathString, offsetString, byteString);
                    receive(network.receive());
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
        }
        return false;
    }

    // Retrieve txt file on the server, write to file and send to server for
    // updating
    public void writeToFile() {
        int offset;

        // Display all files
        if (listAllFiles()) {

            // If successfully displayed
            System.out.printf("Enter file path: ");
            String filePathString = scanner.nextLine();
            String offsetString;

            // Offset input
            while (true) {
                System.out.printf("Enter offset(bytes): ");
                offsetString = scanner.nextLine();
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

            // Data to insert into the file
            System.out.printf("Write: ");
            String writeString = scanner.nextLine();

            // Send packet
            try {
                network.send(2, filePathString, offsetString, writeString);
                cacheManager.addToCache(filePathString, offset, writeString);
                receive(network.receive());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // List all files in the system
    public boolean listAllFiles() {
        try {
            network.send(3);
            return receive(network.receive());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Retrieve txt file on the server and read
    public void monitorUpdates() {
        try {
            // Select file
            int duration = 0;
            System.out.printf("Select File to monitor: ");
            String filePath = scanner.nextLine();
            String durationString;
            while (true) {
                System.out.printf("Select Duration to monitor: ");
                durationString = scanner.nextLine();
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

            // Send packet
            network.send(4, filePath, durationString);
            receive(network.receive());
            long currentTime = System.currentTimeMillis();
            long endTime = currentTime + duration * 1000;
            while (true) {
                DatagramPacket receivePacket = network.waitReceive();

                if (receivePacket == null) {
                    if (System.currentTimeMillis() > endTime) {
                        System.out.println("Ending Monitoring");
                        break;
                    }
                    continue;
                }
                String[] unmarshalledStrings = new Marshaller().unmarshal(receivePacket.getData());
                String message = unmarshalledStrings[2];
                if (Integer.parseInt(unmarshalledStrings[1]) != 0) {
                    continue;
                }
                System.out.println(
                        "Monitoring received with funcId: " + unmarshalledStrings[1] + " | lastModifiedTime: "
                                + Helper.convertLastModifiedTime(Long.parseLong(unmarshalledStrings[4])));
                cacheManager.clearAndReplaceCache(filePath, message, Long.parseLong(unmarshalledStrings[4]));
                network.sendAck(7, filePath);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean deleteFile() {
        try {
            String delete;
            if (listAllFiles()) {
                System.out.printf("Enter file path: ");
                String filePathString = scanner.nextLine();
                System.out.printf("Are you sure? (y/n): ");
                delete = scanner.nextLine();
                if (delete.equalsIgnoreCase("y")) {
                    network.send(5, filePathString);
                    receive(network.receive());
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean insertFile() {
        try {
            System.out.printf("Enter filepath(local file) to insert: ");
            String filePathString = scanner.nextLine();
            System.out.printf("Enter filepath(remote file): ");
            String insertPath = scanner.nextLine();
            String content = new String(Files.readAllBytes(Paths.get(filePathString)));
            network.send(6, insertPath, content);
            receive(network.receive());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
