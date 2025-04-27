package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

class Holder {
    static ConcurrentHashMap<Integer, String> ids_login = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, Object> files_locks = new ConcurrentHashMap<>();

}

class Lock {
    static Semaphore broadcastAccess = new Semaphore(1);

    static Semaphore lock_sem = new Semaphore(1);
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {
    private final int MAX_PACKET_SIZE = 512; // Maximum size of a TFTP packet
    private final Opcode opcode = new Opcode();
    private int connectionId;
    private Connections<byte[]> connections;
    private boolean loggedIn;
    private String username;
    private int lastBlockNumberReceived;
    private int lastBlockNumberSent;
    private String currentFilename;
    private File filesFolder;
    private int ANKBlockNum;
    private Semaphore lock = new Semaphore(1);
    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
        this.loggedIn = false;
        this.username = null;
        this.lastBlockNumberReceived = 0;
        this.lastBlockNumberSent = 0;
        this.currentFilename = null;
        // Create a File object representing the file to write in the "Files" folder
        this.filesFolder = new File("server/Flies");
        if (!filesFolder.exists()) {
            filesFolder.mkdir(); // Create the folder if it doesn't exist
        }
        this.ANKBlockNum = 0;
    }

    @Override
    public boolean shouldTerminate() {
        if (username!=null && !loggedIn) {
            Holder.ids_login.remove(connectionId);
            return true;
        }
        return false;
    }

    @Override
    public void process(byte[] message) {
        short opcodeValue = (short) ((message[0] << 8) | (message[1] & 0xFF)); // Extract opcode from message
        opcode.updatePacketType(opcodeValue); // Update opcode

        if (shouldTerminate())
            return;

        switch (opcode.getPacketType()) {
            case Login_Request:
                handleLoginRequest(message);
                break;
            case Write_Request:
                handleWriteRequest(message);
                break;
            case Read_Request:
                handleReadRequest(message);
                break;
            case Directory_Listing_Request:
                handleDirectoryListingRequest();
                break;
            case Delete_File_Request:
                handleDeleteFileRequest(message);
                break;
            case Data_Packet:
                handleDataPacket(message);
                break;
            case Disconnect:
                handleDisconnect();
                break;
            case Acknowledgment:
                handleAcknowledgment(message);
                break;
            case Error:
                handleErrorPacket(message);
            default:
                break;
        }
    }

    // Helper method to send an ACK packet
    private void sendAckPacket(short blockNumber) {
        byte[] ackPacket = new byte[4];
        ackPacket[0] = 0;
        ackPacket[1] = 4;
        ackPacket[2] = (byte) (blockNumber >> 8);
        ackPacket[3] = (byte) blockNumber;
        connections.send(connectionId, ackPacket);
    }

    private void sendBroadcastPacket(byte[] fileNameBytes, int operation) {
        try {
            lock.acquire();
            int packetSize = fileNameBytes.length + 4; // Size of BCAST packet excluding opcode
            byte[] packet = new byte[packetSize];

            // Set opcode for BCAST packet
            packet[0] = 0;
            packet[1] = 9;

            // Set delete/add operation
            packet[2] = (byte) operation;

            // Copy filename into packet
            System.arraycopy(fileNameBytes, 0, packet, 3, fileNameBytes.length);

            // Terminate the filename with a zero byte
            packet[packetSize - 1] = 0;
            for (Integer id : Holder.ids_login.keySet()) {
                connections.send(id, packet);
            }
            lock.release();
        } catch (InterruptedException e) {lock.release();}
        finally {lock.release();}

    }

    /**Helper method to send an ERROR packet*/
    private void sendErrorPacket(short errorNumber) {
        byte[] errorMessage = getErrorMessage(errorNumber);
        int errorMessageLength = errorMessage.length;

        byte[] errorPacket = new byte[4 + errorMessageLength + 1];
        errorPacket[0] = 0;
        errorPacket[1] = 5;
        errorPacket[2] = 0;
        errorPacket[3] = (byte) errorNumber;
        errorPacket[4 + errorMessageLength] = 0;
        System.arraycopy(errorMessage, 0, errorPacket, 4, errorMessageLength);
        connections.send(connectionId, errorPacket);
    }

    private void sendDataPacket(byte[] data, int blockNumber) {
        int packetSize = data.length; // Size of DATA packet excluding opcode
        byte[] packet = new byte[packetSize + 6];

        packet[0] = 0;
        packet[1] = 3;

        // Set packet size
        packet[2] = (byte) ((packetSize >> 8) & 0xFF);
        packet[3] = (byte) (packetSize & 0xFF);

        // Set block number
        packet[4] = (byte) ((blockNumber >> 8) & 0xFF);
        packet[5] = (byte) (blockNumber & 0xFF);

        // Copy data into packet
        System.arraycopy(data, 0, packet, 6, data.length);

        // Send packet to connection
        connections.send(connectionId, packet);
    }

    private byte[] getErrorMessage(short errorNumber) {
        String errorMessage;
        switch (errorNumber) {
            case 0:
                errorMessage = "Not defined, see error message (if any).";
                break;
            case 1:
                errorMessage = "File not found - RRQ DELRQ of non-existing file.";
                break;
            case 2:
                errorMessage = "Access violation - File cannot be written, read, or deleted.";
                break;
            case 3:
                errorMessage = "Disk full or allocation exceeded - No room in disk.";
                break;
            case 4:
                errorMessage = "Illegal TFTP operation - Unknown Opcode.";
                break;
            case 5:
                errorMessage = "File already exists - File name exists on WRQ.";
                break;
            case 6:
                errorMessage = "User not logged in - Any opcode received before Login completes.";
                break;
            case 7:
                errorMessage = "User already logged in - Login username already connected.";
                break;
            default:
                errorMessage = "Unknown error.";
        }
        return errorMessage.getBytes(StandardCharsets.UTF_8);
    }
    private void handleLoginRequest(byte[] message) {
        //User already logged in
        if (loggedIn) {
            sendErrorPacket((short) 7);
            return;
        }

        // Extract username from message and check if it's already in use
        String username = new String(message, 2, message.length - 2, StandardCharsets.UTF_8);
        boolean isUsernameUsed = Holder.ids_login.containsValue(username);

        if (isUsernameUsed) {
            sendErrorPacket((short) 7);
        } else {
            loggedIn = true;
            this.username = username;
            Holder.ids_login.put(connectionId, username);
            sendAckPacket((short) 0); // ACK with block number 0
        }
    }

    private void handleDeleteFileRequest(byte[] message) {
        if (!loggedIn) {
            sendErrorPacket((short) 6);
            return;
        }

        // Extract filename from the message
        String filename = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
        if(filename.isEmpty() || filename.contains("\0")) {
            sendErrorPacket((short) 0);
            return;
        }

        // Create a File object representing the file to delete
        File fileToDelete = new File("server/Flies/" + filename);
        if (!fileToDelete.exists()) {
            // File not found
            sendErrorPacket((short) 1);
            return;
        }
        if (fileToDelete.delete()) {
            sendAckPacket((short) 0);
            sendBroadcastPacket(filename.getBytes(StandardCharsets.UTF_8), 0);

        } else {
            sendErrorPacket((short) 2); // Access violation or other error during deletion
        }
    }

    private void handleWriteRequest(byte[] message) {
        if (!loggedIn) {
            sendErrorPacket((short) 6);
            return;
        }

        // Extract filename from the message
        String filename = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
        ;
        if (filename.isEmpty() || filename.contains("\0")) {
            sendErrorPacket((short) 0);
            return;
        }
        // get folder path
        String folderPath = "./server/Flies/";
        File folder = new File("server/Flies");

        if (folder.exists() && folder.isDirectory()) {
            folderPath = folder.getAbsolutePath();
            System.out.println("Folder exists at: " + folderPath);
        } else {
            System.out.println("Folder does not exist.");
        }

        File fileToWrite = new File(folderPath , filename);
        if (fileToWrite.exists())
        {
            sendErrorPacket((short) 5); // File already exists
            return;
        }
        if (!fileToWrite.exists())
        {
            try {
                FileOutputStream fileoutputStream = new FileOutputStream(fileToWrite);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            Holder.files_locks.putIfAbsent(filename, new Object());
            currentFilename = filename;
            lastBlockNumberReceived = 0;
            sendAckPacket((short) 0);
        }
    }

    private void handleDataPacket(byte[] message) {
        if (!loggedIn) {
            sendErrorPacket((short) 6);
            return;
        }

        // Extract packet size and block number
        short packetSize = (short)((message[2] << 8) | (message[3] & 0xFF));
        short blockNumber = (short)((message[4] << 8) | (message[5] & 0xFF));

        if (packetSize > MAX_PACKET_SIZE) {
            sendErrorPacket((short) 3); // Disk full or allocation exceeded
            return;
        }

        if (blockNumber != lastBlockNumberReceived+1) {
            sendErrorPacket((short) 4);
            return;
        }

        for (int i = 0; i < message.length; i++)
            System.out.print(message[i] + " ");

        try (FileOutputStream file  = new FileOutputStream(currentFilename, true)) {
            file.write(message, 6, packetSize);
        } catch (IOException e) {
            sendErrorPacket((short) 2);
        }
        // Last packet
        if (packetSize < MAX_PACKET_SIZE) {
            sendAckPacket(blockNumber);
            lastBlockNumberReceived = 0;

            sendBroadcastPacket(currentFilename.getBytes(StandardCharsets.UTF_8), 1);
            currentFilename = null;
        }
        else {
            lastBlockNumberReceived++;
            sendAckPacket(blockNumber);
        }

    }

    private void handleDirectoryListingRequest() {
        if (!loggedIn) {
            sendErrorPacket((short) 6);
            return;
        }

        // Extract directory listing
        if (!filesFolder.exists()) {
            sendErrorPacket((short) 2); // Access violation or directory not found
            return;
        }
        File[] directory = filesFolder.listFiles();
        if (directory == null || directory.length == 0) {
            // Send an empty data packet
            sendDataPacket(new byte[0], (short) 1);
            return;
        }

        ANKBlockNum = 0;

        ByteArrayOutputStream messageBytes = new ByteArrayOutputStream();
        int dataLength = 0;

        // Prepare directory listing:
        for (File file : directory) {
            if (file.isFile()) {
                byte[] fileName = (file.getName() + '\0').getBytes(StandardCharsets.UTF_8);
                dataLength += fileName.length;
                messageBytes.write(fileName, 0, fileName.length);
            }
        }

        byte[] data = messageBytes.toByteArray();
        int remainingBytes = dataLength;
        int blockNumber = 1;

        int index = 0;
        while (remainingBytes > 0) {
            int length = Math.min(MAX_PACKET_SIZE, remainingBytes);
            byte[] packet = new byte[length];
            System.arraycopy(data, index, packet, 0, length);
            sendDataPacket(packet, (short) blockNumber);
            blockNumber++;
            index += length;
            remainingBytes -= length;
        }
    }
    /**User not logged in, send error packet and return*/
    private void handleDisconnect() {
        if (!loggedIn) {
            sendErrorPacket((short) 6);
            return;
        }

        // Send ACK packet to acknowledge disconnect request
        sendAckPacket((short) 0); // ACK with block number 0
        // Remove user from the set of logged-in users
        Holder.ids_login.remove(connectionId);
        loggedIn = false;
    }

    private void handleReadRequest(byte[] message) {
        if (!loggedIn) {
            sendErrorPacket((short) 6); // User not logged in
            return;
        }

        // Extract filename from the message
        String filename = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);

        if(filename.isEmpty() || filename.contains("\0")) {
            sendErrorPacket((short) 0); // Undefined opcode
            return;
        }

        // Create a File object representing the file to read
        File fileToRead = new File("server/Flies/" + filename);

        // Check if file exists
        if (!fileToRead.exists()) {
            sendErrorPacket((short) 1); // File not found
            return;
        }

        // Read file contents
        try (FileInputStream fis = new FileInputStream(fileToRead)) {
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            int bytesRead;
            lastBlockNumberSent = 1;
            currentFilename = filename;
            while ((bytesRead = fis.read(buffer)) != -1) {
                // Send data packet
                byte[] data = Arrays.copyOf(buffer, bytesRead);
                sendDataPacket(data, lastBlockNumberSent);
                lastBlockNumberSent++;
            }
        } catch (IOException e) {
            lastBlockNumberSent = 0;
            sendErrorPacket((short) 2); // Access violation or other error during file reading
            currentFilename = null;
        }
    }

    private void handleAcknowledgment(byte[] message) {
        if (!loggedIn) {
            sendErrorPacket((short) 6); // User not logged in
            return;
        }

        short blockNum = (short)((message[2] << 8) | (message[3] & 0xFF));

        if (blockNum != ANKBlockNum + 1) {
            System.out.println("blockNum: " + blockNum + " ANKBlockNum: " + ANKBlockNum);
            sendErrorPacket((short) 4); // Illegal TFTP operation
            return;
        }

        ANKBlockNum = blockNum;
    }

    private void handleErrorPacket(byte[] packet) {
        short errorNumber = (short) ((packet[2] << 8) | (packet[3] & 0xFF));
        String msg = new String(packet, 4, packet.length - 5, StandardCharsets.UTF_8);
        System.out.println("Error " + errorNumber + " (" + msg + ")");
    }

}