package server;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.channels.Channel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import client.UDPClient.CallbackObject;

import java.util.Hashtable;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;

import common.Marshaller;

public class UDPServer {
    public static void main(String[] args) {
        DatagramSocket serverSocket = null;
        Marshaller marshaller = new Marshaller();
        try {
            // Create a UDP socket
            serverSocket = new DatagramSocket(Integer.parseInt(args[0])); // Port number can be any available port

            byte[] receiveData = new byte[1024];

            Dictionary<String, List<Callback>> registry = new Hashtable<>(); // Creating registry for monitoring updates
            Comparator<Callback> CallbackComparator = new Comparator<Callback>() { // Adding a comparator for Priority
                                                                                   // Queue
                @Override
                public int compare(Callback c1, Callback c2) {
                    return Long.compare(c1.EndTime, c2.EndTime);
                }
            };
            PriorityQueue<Callback> callbackQueue = new PriorityQueue<Callback>(CallbackComparator);
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                byte[] returnedMessage;
                DatagramPacket sendPacket;

                // Regularly check Priority Queue to remove overdue monitoring
                long currentTime = System.currentTimeMillis();
                while (!callbackQueue.isEmpty() && (currentTime > callbackQueue.element().EndTime)) {
                    Callback toRemove = callbackQueue.remove();
                    // Send a packet to the client to inform that callback has ended
                    returnedMessage = marshaller.marshal(3, "End");
                    sendPacket = new DatagramPacket(returnedMessage, returnedMessage.length, toRemove.ClientAdd,
                            toRemove.ClientPort);
                    serverSocket.send(sendPacket);
                }
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

                switch (clientChosen) {
                    case 1:
                        String fileContent = readFile(unmarshalledStrings);
                        returnedMessage = marshaller.marshal(1, fileContent);
                        TimeUnit.SECONDS.sleep(5);
                        sendPacket = new DatagramPacket(returnedMessage, returnedMessage.length, clientAddress,
                                clientPort);
                        serverSocket.send(sendPacket);
                        break;
                    case 3:
                        String filePath = unmarshalledStrings[1];
                        int duration = Integer.parseInt(unmarshalledStrings[3]); // Duration in seconds
                        Callback callback = new Callback(clientAddress, clientPort, currentTime + duration);
                        callbackQueue.add(callback);
                        // Adding into the registry
                        if (registry.get(filePath) == null) {
                            registry.put(filePath, new ArrayList<Callback>());
                        } else {
                            List<Callback> currentArray = registry.get(filePath);
                            currentArray.add(callback);
                            registry.put(filePath, currentArray);
                        }
                        break;

                    default:
                        break;
                }
                // Sending poll to the
                List<Callback> clientList = registry.get(unmarshalledStrings[1]);
                if (clientList != null) {
                    for (Callback callback : clientList) {
                        returnedMessage = marshaller.marshal(3, "file has been updated");
                        sendPacket = new DatagramPacket(returnedMessage, returnedMessage.length, callback.ClientAdd,
                                callback.ClientPort);
                        serverSocket.send(sendPacket);
                    }
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

    // Callback Object
    public static class Callback {
        InetAddress ClientAdd;
        int ClientPort;
        long EndTime;

        public Callback(InetAddress clientAdd, int clientPort, long EndTime) {
            this.ClientAdd = clientAdd;
            this.ClientPort = clientPort;
            this.EndTime = EndTime;
        }
    }

}