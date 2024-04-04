package server;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.TimerTask;

import common.Helper;
import common.Marshaller;
import common.NetworkServer;

public class UDPServer {

    // hashmap for storing lastModified of each file
    FileManager fileManager;
    Marshaller marshaller;

    public static void main(String[] args) {
        // Server setup
        boolean isAtMostOnce = args[1].equals("1");
        UDPServer udpServer = new UDPServer();
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
            udpServer.startServer(isAtMostOnce, serverSocket);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }

    public void startServer(boolean isAtMostOnce, DatagramSocket serverSocket) {
        fileManager = new FileManager();
        marshaller = new Marshaller();

        try {
            fileManager = new FileManager();
            // Create a UDP socket
            NetworkServer serverHandler = new NetworkServer(serverSocket, isAtMostOnce);
            Dictionary<String, List<Callback>> registry = new Hashtable<>(); // Creating registry for monitoring updates

            // Manually populate fileManager for existing files
            Long timenow = System.currentTimeMillis();
            String[] cacheFiles = listFiles().split("\n");
            for (int i = 0; i < cacheFiles.length; i++) {
                fileManager.addFile(cacheFiles[i], timenow);
            }

            // Server process
            while (true) {
                fileManager.printAllFiles();

                // Retrieve data from socket
                DatagramPacket receivePacket = serverHandler.receive();
                if (receivePacket == null) {
                    continue;
                }

                // Get client's address and port number
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                // Process data
                byte[] receivedMessage = receivePacket.getData();
                String[] unmarshalledStrings = marshaller.unmarshal(receivedMessage);

                int clientChosen = Integer.parseInt(unmarshalledStrings[0]);
                String fileContent;

                // Functions for Client to call
                switch (clientChosen) {
                    case 1:
                        fileContent = readFile(unmarshalledStrings);
                        System.out.println("CLIENT READ: SENDING THE FOLLOWING - "
                                + Arrays.toString(unmarshalledStrings) + " | fileContent and LastModifiedTime: "
                                + String.valueOf(fileManager.getLastModifiedTime(unmarshalledStrings[2])));

                        // File content and Cache Response
                        serverHandler.reply(receivePacket, 1,
                                unmarshalledStrings[2],
                                unmarshalledStrings[3],
                                unmarshalledStrings[4],
                                fileContent,
                                String.valueOf(fileManager.getLastModifiedTime(unmarshalledStrings[2])));
                        break;
                    case 2:
                        fileContent = writeToFile(unmarshalledStrings);

                        // to ensure lastModifiedTime is the same, server will update and send the time
                        // back to client
                        Long lastModifiedTime = System.currentTimeMillis();
                        fileManager.updateLastModifiedTime(unmarshalledStrings[2], lastModifiedTime);

                        String fileName = unmarshalledStrings[2];
                        serverHandler.reply(receivePacket, 4, "Name successfully updated", fileName,
                                String.valueOf(lastModifiedTime), unmarshalledStrings[4]);

                        List<Callback> clients = registry.get("bin/resources/" + fileName);
                        if (clients != null) {
                            List<Callback> clientList = new ArrayList<Callback>(clients);
                            byte[] sendData = marshaller.marshal(4, 0,
                                    fileContent, fileName,
                                    String.valueOf(lastModifiedTime));
                            serverHandler.newUpdate(sendData, fileName, clientList);
                        }
                        break;
                    case 3:
                        // Retrieve a list of files on the Server recursively
                        fileContent = listFiles();
                        serverHandler.reply(receivePacket, 2, fileContent);
                        break;
                    case 4:
                        try {
                            String filePath = "bin/resources/" + unmarshalledStrings[2];
                            File file = new File(filePath);
                            // Checking if file exists
                            if (!file.exists()) {
                                throw new FileNotFoundException("File does not exist on server");
                            }
                            int duration = Integer.parseInt(unmarshalledStrings[3]); // Duration in seconds
                            Callback callback = new Callback(clientAddress, clientPort);
                            // Creating a new thread to remove client from registry once done
                            new Thread() {
                                public void run() {
                                    try {
                                        Thread.sleep(duration * 1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    registry.get(filePath).remove(callback);
                                }
                            }.start();
                            // Adding into the registry
                            if (registry.get(filePath) == null) {
                                List<Callback> currentArray = new ArrayList<Callback>();
                                currentArray.add(callback);
                                registry.put(filePath, currentArray);
                            } else {
                                List<Callback> currentArray = registry.get(filePath);
                                currentArray.add(callback);
                                registry.put(filePath, currentArray);
                            }
                            fileContent = "Acknowledged! Client added to monitoring mode";

                        } catch (FileNotFoundException e) {
                            fileContent = "404:Error! File Not Found";
                        }
                        serverHandler.reply(receivePacket, 3, fileContent);
                        break;
                    case 5:
                        // client delete file
                        fileContent = deleteFile(unmarshalledStrings);

                        // client should only clear cache for file if server replies.
                        serverHandler.reply(receivePacket, 5, fileContent, unmarshalledStrings[2]);
                        break;
                    case 6:
                        Long timeNow = System.currentTimeMillis();
                        // client insert file
                        fileContent = insertFile(unmarshalledStrings, timeNow);

                        // server should return lastModifiedTime, client cache should be empty. need to
                        // test if that causes issues.
                        serverHandler.reply(receivePacket, 6, fileContent, unmarshalledStrings[3],
                                String.valueOf(timeNow));
                        break;
                    // General Acknowledgement
                    case 7:
                        Callback client = new Callback(receivePacket.getAddress(), receivePacket.getPort());
                        serverHandler.removeClient(unmarshalledStrings[2], client);
                        System.out.println("Ack Received!");
                        break;
                    case 10:
                        // for client to request modified time of file
                        System.err.println(Arrays.toString(unmarshalledStrings));
                        Long lastModified = fileManager.getLastModifiedTime(unmarshalledStrings[2]);
                        if (lastModified != null) {
                            serverHandler.reply(receivePacket, 1, String.valueOf(lastModified));
                        } else {
                            serverHandler.reply(receivePacket, 1, "Missing file in server");
                        }
                        break;

                    default:
                        System.out.println(
                                "Received an invalid funcID from " + receivePacket.getSocketAddress().toString());
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

    public static String readFile(String[] unmarshalledStrings) {
        String filePath = "bin/resources/" + unmarshalledStrings[2];
        int offset = Integer.valueOf(unmarshalledStrings[3]);
        int noOfBytes = Integer.valueOf(unmarshalledStrings[4].trim()); // Number of bytes to read
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
            content = "404:File does not exist on server.";
        } catch (EOFException e) {
            content = "404:End of file error.";
        } catch (IOException e) {
            content = "404:IOException Error";
            e.printStackTrace();
        }
        return content;
    }

    public static String writeToFile(String[] unmarshalledStrings) {
        String filePath = "bin/resources/" + unmarshalledStrings[2];
        int offset = Integer.valueOf(unmarshalledStrings[3]);
        String write = unmarshalledStrings[4].trim();
        String content = "No content in file";
        System.out.println("WRITING " + write + " TO " + unmarshalledStrings[2]);
        try (
                // Read file content
                RandomAccessFile file = new RandomAccessFile(filePath, "rw")) {
            file.seek(offset);

            // Read data after offset
            StringBuilder restOfFile = new StringBuilder();
            int data;
            while ((data = file.read()) != -1) {
                restOfFile.append((char) data);
            }

            // Write to offset
            file.seek(offset);
            file.write(write.getBytes());

            // Write old data to the end of the new data
            file.seek(offset + write.length());
            file.writeBytes(restOfFile.toString());

            // Returns the updated file
            file.seek(0);
            String readValue = "";
            while (file.getFilePointer() < file.length()) {
                readValue += file.readLine();
            }
            file.close();

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

    public String deleteFile(String[] unmarshalledStrings) {
        String filePath = "bin/resources/" + unmarshalledStrings[2];
        String content = "";

        try {
            // Find file
            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException("File does not exist on server");
            }

            if (file.delete()) {
                content = "File deleted";
                fileManager.removeFile(unmarshalledStrings[2]);
            }
        } catch (FileNotFoundException e) {
            content = e.getMessage();
        }
        return content;
    }

    public String insertFile(String[] unmarshalledStrings, Long lastModifiedTime) {
        String filePath = "bin/resources/" + unmarshalledStrings[2];
        String fileContent = unmarshalledStrings[3];
        String content = null;
        File file;
        RandomAccessFile raf;
        String fileName = null;

        try {
            while (true) {
                file = new File(filePath);
                if (file.createNewFile()) {
                    raf = new RandomAccessFile(file, "rw");
                    raf.seek(0);
                    raf.write(fileContent.getBytes());
                    raf.close();
                    fileName = unmarshalledStrings[2];
                    break;
                } else {
                    String fileType = "";

                    // Last . refers to file type
                    int dotIndex = filePath.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < filePath.length() - 1) {
                        // Retrieve file type
                        fileType = filePath.substring(dotIndex + 1).toLowerCase();
                        // Retrieve path till file type
                        filePath = filePath.substring(0, dotIndex);
                    }

                    // Append _copy to fileNameWithoutExtension
                    filePath = filePath + "_copy." + fileType;
                    String fileNameWithoutExtension = Helper.extractFileName(unmarshalledStrings[2]);
                    fileName = fileNameWithoutExtension + "_copy." + fileType;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fileName != null) {
            fileManager.addFile(fileName, lastModifiedTime);
            content = fileName;
        }
        return content;
    }

    // Callback Object
    public static class Callback {
        InetAddress ClientAdd;
        int ClientPort;

        public Callback(InetAddress clientAdd, int clientPort) {
            this.ClientAdd = clientAdd;
            this.ClientPort = clientPort;
        }

        public InetAddress getAdd() {
            return ClientAdd;
        }

        public int getPort() {
            return ClientPort;
        }
    }

    public static class removeMonitor extends TimerTask {
        private Callback callback;
        private Dictionary<String, List<Callback>> registry;
        private String filePath;

        removeMonitor(Callback callback, Dictionary<String, List<Callback>> registry, String filePath) {
            this.callback = callback;
            this.registry = registry;
            this.filePath = filePath;
        }

        @Override
        public void run() {
            registry.get(filePath).remove(callback);
            System.out.println("This timer is cancelled");
            this.cancel();
        }
    }

    public static String listFiles() { // Returns the file paths without the base directory
        String base = "bin\\resources\\";
        File folder = new File(base);
        return listFilesInFolder(folder).replace(base, "");
    }

    private static String listFilesInFolder(File folder) { // Recursive loop through folder
        String files = "";
        for (final File file : folder.listFiles()) {
            if (file.isDirectory()) {
                files += listFilesInFolder(file);
            } else {
                files += file.getPath() + "\n";
            }
        }
        return files;
    }
}