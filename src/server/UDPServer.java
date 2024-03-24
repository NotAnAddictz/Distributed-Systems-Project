package server;

import java.io.EOFException;
import java.io.File;
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
            while (true) {
                byte[] receiveData = new byte[1024];
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
                String fileContent = "";
                
                switch (clientChosen) {
                    case 1:
                        fileContent = readFile(unmarshalledStrings);
                        returnedMessage = marshaller.marshal(1, fileContent);
                        TimeUnit.SECONDS.sleep(1);
                        sendPacket = new DatagramPacket(returnedMessage, returnedMessage.length, clientAddress, clientPort);
                        serverSocket.send(sendPacket);
                        break;
                    case 2:
                        fileContent = writeToFile(unmarshalledStrings);
                        returnedMessage = marshaller.marshal(1, fileContent);
                        TimeUnit.SECONDS.sleep(1);
                        sendPacket = new DatagramPacket(returnedMessage, returnedMessage.length, clientAddress, clientPort);
                        serverSocket.send(sendPacket);
                        break;
                    case 4:
                        fileContent = listFiles(unmarshalledStrings);
                        returnedMessage = marshaller.marshal(2, fileContent);
                        TimeUnit.SECONDS.sleep(1);
                        sendPacket = new DatagramPacket(returnedMessage, returnedMessage.length, clientAddress, clientPort);
                        serverSocket.send(sendPacket);
                        break;
                    default:
                        System.out.println("Received an invalid funcID from " + receivePacket.getSocketAddress().toString());
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
        String filePath = "bin/resources/" + unmarshalledStrings[1];
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

    public static String writeToFile(String[] unmarshalledStrings){
        String filePath = "bin/resources/" + unmarshalledStrings[1];
        int offset = Integer.valueOf(unmarshalledStrings[2]);
        String write = unmarshalledStrings[3].trim();
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