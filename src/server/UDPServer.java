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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import common.Helper;
import common.Marshaller;
import common.NetworkServer;

public class UDPServer {
    public static void main(String[] args) {
        boolean isAtMostOnce = args[1].equals("1");
        DatagramSocket serverSocket = null;
        Marshaller marshaller = new Marshaller();

        try {
            // Create a UDP socket
            serverSocket = new DatagramSocket(Integer.parseInt(args[0])); // Port number can be any available port
            NetworkServer serverHandler = new NetworkServer(serverSocket, isAtMostOnce);
            Dictionary<String, List<Callback>> registry = new Hashtable<>(); // Creating registry for monitoring updates

            // hashmap for storing lastModified of each file
            Map<String, Long> lastModifiedMap = new HashMap<>();

            // manually populate for existing files
            Long timenow = System.currentTimeMillis();
            lastModifiedMap.put("file1.txt", timenow);
            lastModifiedMap.put("file2.txt", timenow);
            lastModifiedMap.put("file3.txt", timenow);
            lastModifiedMap.put("testfile.txt", timenow);

            while (true) {
                Helper.printFileLastModifiedTime(lastModifiedMap);
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                // Receive packet from client
                serverSocket.receive(receivePacket);
                System.out.println("Packet Received from: " + receivePacket.getAddress() + " Client Port: "
                        + receivePacket.getPort());

                // Get client's address and port number
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                byte[] receivedMessage = receivePacket.getData();
                String[] unmarshalledStrings = marshaller.unmarshal(receivedMessage);

                int clientChosen = Integer.parseInt(unmarshalledStrings[0]);
                String fileContent;
                switch (clientChosen) {
                    case 1:
                        fileContent = readFile(unmarshalledStrings);
                        System.out.println("CLIENT READ: SENDING THE FOLLOWING - "
                                + Arrays.toString(unmarshalledStrings) + " | fileContent and LastModifiedTime: "
                                + String.valueOf(lastModifiedMap.get(unmarshalledStrings[1])));
                        serverHandler.reply(receivePacket, 1,
                                unmarshalledStrings[2],
                                unmarshalledStrings[3],
                                unmarshalledStrings[4],
                                fileContent,
                                String.valueOf(lastModifiedMap.get(unmarshalledStrings[2])));
                        // funcId, packetid, filename, offset, readBytes, filecontent, time
                        break;
                    case 2:
                        fileContent = writeToFile(unmarshalledStrings);

                        // to ensure lastModifiedTime is the same, server will update and send the time
                        // back to client
                        Long lastModifiedTime = System.currentTimeMillis();
                        lastModifiedMap.put(unmarshalledStrings[2], lastModifiedTime);
                        String file = unmarshalledStrings[2];
                        serverHandler.reply(receivePacket, 4, "File successfully updated", file,
                                String.valueOf(lastModifiedTime));
                        List<Callback> clients = registry.get("bin/resources/" + file);
                        if (clients != null) {
                            List<Callback> clientList = new ArrayList<Callback>(clients);
                            byte[] sendData = marshaller.marshal(4, 0,
                                    fileContent, file,
                                    String.valueOf(lastModifiedTime));
                            serverHandler.newUpdate(sendData, file, clientList);
                        }
                        break;
                    case 3:
                        fileContent = listFiles(unmarshalledStrings);
                        serverHandler.reply(receivePacket, 2, fileContent);
                        break;
                    case 4:
                        String filePath = "bin/resources/" + unmarshalledStrings[2];
                        int duration = Integer.parseInt(unmarshalledStrings[3]); // Duration in seconds
                        Callback callback = new Callback(clientAddress, clientPort);
                        new java.util.Timer().schedule(new removeMonitor(callback, registry, filePath),
                                duration * 1000);
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
                        serverHandler.reply(receivePacket, 3, fileContent);

                        break;
                    case 5:
                        fileContent = deleteFile(unmarshalledStrings);
                        serverHandler.reply(receivePacket, 1, fileContent);
                        break;
                    case 6:
                        fileContent = insertFile(unmarshalledStrings);
                        serverHandler.reply(receivePacket, 1, fileContent);
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
                        Long lastModified = lastModifiedMap.get(unmarshalledStrings[2]);
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
        } catch (

        Exception e) {
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

    public static String deleteFile(String[] unmarshalledStrings) {
        String filePath = "bin/resources/" + unmarshalledStrings[1];
        String content = "";

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException("File does not exist on server");
            }
            if (file.delete()) {
                content = "File deleted";
            }
        } catch (FileNotFoundException e) {
            content = e.getMessage();
        }
        return content;
    }

    public static String insertFile(String[] unmarshalledStrings) {
        String filePath = "bin/resources/" + unmarshalledStrings[1];
        String fileContent = unmarshalledStrings[2];
        String content = fileContent;
        File file;
        RandomAccessFile raf;

        try {
            while (true) {
                file = new File(filePath);
                if (file.createNewFile()) {
                    raf = new RandomAccessFile(file, "rw");
                    raf.seek(0);
                    raf.write(fileContent.getBytes());
                    raf.close();
                    content = "" + filePath + " has been created in the file system";
                    break;
                } else {
                    String fileType = "";
                    int dotIndex = filePath.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < filePath.length() - 1) {
                        fileType = filePath.substring(dotIndex + 1).toLowerCase();
                        filePath = filePath.substring(0, dotIndex);
                    }
                    filePath = filePath + "_copy." + fileType;
                }
            }
        } catch (FileNotFoundException e) {
            content = e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
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
            cancel();
        }
    }

    public static String listFiles(String[] unmarshalledStrings) {
        File folder = new File("bin/resources/");
        return listFilesInFolder(folder, 0);
    }

    private static String listFilesInFolder(File folder, int depth) {
        String files = "";
        for (final File file : folder.listFiles()) {
            if (file.isDirectory()) {
                listFilesInFolder(file, depth + 1);
            } else {
                files += " ".repeat(depth) + file.getName() + "\n";
            }
        }
        return files;
    }
}