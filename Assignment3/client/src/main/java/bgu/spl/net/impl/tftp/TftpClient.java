package bgu.spl.net.impl.tftp;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

enum dataMode {
    UNDIFFERENTIATED, RRQ, DIRQ, WRQ
}

public class TftpClient {
    private final Socket socket;
    //read from the client and server :
    private final BufferedReader keyboardReader;
    private final BufferedOutputStream serverWriter; //out
    private final BufferedInputStream serverReader; //in
    private final OutputStream outputStream;
    private boolean terminate; //assume user not loged in yet
    private boolean loggedIn = false; //assume user not loged in yet
    private int lastBlockNumberReceived;
    private final Object commectionLock = new Object();
    private String pendingRRQFileName;
    private String pendingWRQFileName;
    private boolean isWaitingForResponse;
    private String WRQiNProcess;

    dataMode mode;

    public TftpClient(Socket socket) throws IOException {
        this.socket = socket;
        this.keyboardReader = new BufferedReader(new InputStreamReader(System.in));
        this.serverReader = new BufferedInputStream(socket.getInputStream());
        this.serverWriter = new BufferedOutputStream(socket.getOutputStream());
        this.outputStream = socket.getOutputStream();
        this.terminate = false;
        this.lastBlockNumberReceived = 0;
        this.isWaitingForResponse = false;
        this.mode = dataMode.UNDIFFERENTIATED;
        this.pendingRRQFileName = null;
        this.pendingWRQFileName = null;
        this.WRQiNProcess = null;
    }

    public void start() {
        //Start the keyboard input and server response handling threads
        Thread keyboardThread = new Thread(new KeyboardInputHandler());
        Thread listeningThread = new Thread(new ServerResponseHandler());
        keyboardThread.start();
        listeningThread.start();

        System.out.println("Connected to the server!");

        //Wait for the threads to finish before closing the connection
        try {
            keyboardThread.join();
            listeningThread.join();
        } catch (InterruptedException e) {e.printStackTrace();}
    }

    private class KeyboardInputHandler implements Runnable {
        @Override
        public void run() {
            try {
                //Read input from the keyboard
                while (!terminate) {
                    isWaitingForResponse = false;
                    String input = keyboardReader.readLine();
                    if (input != null && !input.isEmpty()) {
                        sendCommand(input);

                        //Wait for the server response
                        if (isWaitingForResponse && !terminate) {
                            try {
                                synchronized (commectionLock) {
                                    commectionLock.wait(); //Waits until the listener thread notifies
                                    isWaitingForResponse = false;
                                }
                            } catch (InterruptedException ignore) {}
                        }
                    }
                }
            } catch (IOException e) {
                isWaitingForResponse = false;
                e.printStackTrace();
            }
        }
    }

    private class ServerResponseHandler implements Runnable {
        @Override
        public void run() {
            try {
                //Read input from the server
                TftpClientEncDec encdec = new TftpClientEncDec();
                int bytesRead;
                while (!terminate && (bytesRead = serverReader.read()) >= 0) {
                    byte[] response = encdec.decodeNextByte((byte) bytesRead);
                    if (response != null) {
                        if (response.length >= 5 && (response[0] == 0 && response[1] == 3)) { //check if the response is data
                            handleDataPacket(response);
                        } else if (response.length >= 4 && (response[0] == 0 && response[1] == 4)) { //check if the response is ack
                            handleAckPacket(response);
                        } else if (response.length > 3 && (response[0] == 0 && response[1] == 9)) { //check if the response is bcast
                            handleBcastPacket(response);
                        } else if (response.length >= 4 && (response[0] == 0 && response[1] == 5)) { //check if the response is error
                            handleErrorPacket(response);
                        }
                        synchronized (commectionLock) {
                            commectionLock.notifyAll(); //Notify the keyboard input thread that response was received
                        }
                    }
                }
            } catch (IOException e) {
                synchronized (commectionLock) {
                    commectionLock.notifyAll(); // Corrected notification within synchronized block
                }
                e.printStackTrace();
            }
        }
    }

    private void sendCommand(String command) {
        try {
            //Split the command to tokens
            String[] tokens = command.split("\\s+", 2);
            String commandType = tokens[0];
            String data = (tokens.length > 1) ? tokens[1] : "";

            //Handle the command
            switch (commandType) {
                case "LOGRQ":
                    sendLogRq(data);
                    break;
                case "DELRQ":
                    sendDelRq(data);
                    break;
                case "RRQ":
                    lastBlockNumberReceived = 0;
                    sendRrq(data);
                    break;
                case "WRQ":
                    sendWrq(data);
                    break;
                case "DIRQ":
                    lastBlockNumberReceived = 0;
                    sendDirq();
                    break;
                case "DISC":
                    sendDisc();
                    break;
                default:
                    isWaitingForResponse = false;
                    mode = dataMode.UNDIFFERENTIATED;
                    System.out.println("Invalid command");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendLogRq(String username) throws IOException {
        //Check if the username is valid
        if (!isLegalUsername(username)) {
            System.out.println("Invalid command");
            return;
        }

        //Prepare the LOGRQ packet
        byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[3 + usernameBytes.length];
        message[0] = 0; //Opcode for LOGRQ
        message[1] = 7; //Packet size
        message[message.length - 1] = 0;
        System.arraycopy(usernameBytes, 0, message, 2, usernameBytes.length);

        //Update logged-in status
        loggedIn = true;

        //Send the packet
        isWaitingForResponse = true;
        sendToServer(message);
    }

    public static boolean isLegalUsername (String username) {
        return  !(username == null || username.isEmpty() || username.trim().isEmpty());
    }

    private void sendDelRq(String filename) throws IOException {
        //Check if the filename is valid
        if (!isLegalFilename(filename)) {
            System.out.println("Invalid command");
            return;
        }

        //Prepare the DELRQ packet
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[3 + filenameBytes.length];
        message[0] = 0; //Opcode for LOGRQ
        message[1] = 8; //Packet size
        message[message.length - 1] = 0;
        System.arraycopy(filenameBytes, 0, message, 2, filenameBytes.length);

        //Send the packet
        isWaitingForResponse = true;
        sendToServer(message);
    }

    private void sendRrq(String filename) throws IOException {
        //Check if the filename is valid
        if (!isLegalFilename(filename)) {
            System.out.println("Invalid command");
            return;
        }

        //Check if the file already exists
        File newFile = new File(filename);

        //Create the file and check if it already exists
        try {
            if(!newFile.createNewFile()) {
                System.out.println("file already exists");
                return;}
        } catch (IOException e) {
            pendingRRQFileName = null;
            mode = dataMode.UNDIFFERENTIATED;
            e.printStackTrace();
        }

        //Set the mode to RRQ and store the filename
        pendingRRQFileName = filename;
        mode = dataMode.RRQ;

        //Prepare the RRQ packet
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[3 + filenameBytes.length];
        message[0] = 0; // Opcode for LOGRQ
        message[1] = 1; // Packet size
        message[message.length - 1] = 0; // Null-terminator
        System.arraycopy(filenameBytes, 0, message, 2, filenameBytes.length);

        //Send the packet
        isWaitingForResponse = true;
        sendToServer(message);
    }

    public static boolean isLegalFilename (String filename) {
        return  !(filename == null || filename.isEmpty() || filename.trim().isEmpty() ||
                filename.contains("\0") || filename.contains("\n"));
    }

    private void sendDirq() throws IOException {
        //Prepare the DIRQ packet
        byte[] message = {0, 6};
        mode = dataMode.DIRQ;
        lastBlockNumberReceived = 0;

        //Send the packet
        isWaitingForResponse = true;
        sendToServer(message);
    }

    private void sendWrq(String fileName) throws IOException {
        //Check if the filename is valid
        if (!isLegalFilename(fileName)) {
            System.out.println("Invalid command");
            return;
        }

        //Check if the file already exists
        File newFile = new File(fileName);
        if (!newFile.exists()) {
            System.out.println("file does not exists");
            return;
        }

        //Prepare the WRQ packet
        byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[3 + fileNameBytes.length];
        message[0] = 0; //Opcode for WRQ
        message[1] = 2;
        message[message.length - 1] = 0;
        System.arraycopy(fileNameBytes, 0, message, 2, fileNameBytes.length);

        //Set the mode to WRQ and store the filename
        pendingWRQFileName = fileName;
        mode = dataMode.WRQ;

        //Send the packet
        WRQiNProcess = fileName;
        isWaitingForResponse = true;
        sendToServer(message);
    }

    private void sendDisc() throws IOException {
        if (loggedIn) {
            //Prepare the DISC packet and send the packet to the server
            byte[] message = {0, 10};
            sendToServer(message);
            isWaitingForResponse = true;

            //Wait for the server ACK response
            byte[] response = new byte[4];
            if (serverReader.available() >= 4) {
                serverReader.read(response);
                if (response[0] == 0 && response[1] == 4) {
                    closeConnection();
                    System.exit(1);
                }
            }
        }
    }

    private void closeConnection() {
        terminate = true;

        synchronized (commectionLock) {
            commectionLock.notifyAll();
        }
        try {
            if (serverReader != null) serverReader.close(); //Close the server reader
            if (serverWriter != null) serverWriter.close(); //Close the server writer
            if (outputStream != null) outputStream.close(); //Close the output stream
            if (socket != null) socket.close(); //Close the socket

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDataPacket(byte[] packet) {
        //Extracting the block number and data packet size and content from the packet
        short dataPacketSize = (short)((packet[2] << 8) | (packet[3] & 0xFF));
        short blockNumber = (short)((packet[4] << 8) | (packet[5] & 0xFF));
        byte[] data = Arrays.copyOfRange(packet, 6, 6 + dataPacketSize);

        //Check if the data packet size is valid
        if (dataPacketSize > 512) {
            //System.out.println("Invalid data packet size: " + dataPacketSize);
            pendingRRQFileName = null;
            mode = dataMode.UNDIFFERENTIATED;
            return;
        }

        //Check if the block number is valid
        if (blockNumber != lastBlockNumberReceived + 1) {
            //System.out.println("Invalid block number: " + blockNumber);
            pendingRRQFileName = null;
            mode = dataMode.UNDIFFERENTIATED;
        }

        //Handle the RRQ respond data packets
        if (mode == dataMode.RRQ && pendingRRQFileName != null) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(pendingRRQFileName, true)) {
                fileOutputStream.write(data);
                lastBlockNumberReceived++;
            } catch (IOException e) {e.printStackTrace();}
        }

        //Handle the DIRQ respond data packets
        if (mode == dataMode.DIRQ) {
            ArrayList<Byte> filenameBytes = new ArrayList<>();
            for (byte b : data) {
                if (b != 0x0) {
                    filenameBytes.add(b);
                } else {
                    //Convert filenameBytes to a string and print it
                    byte[] filenameBytesArr = new byte[filenameBytes.size()];
                    for (int i = 0; i < filenameBytes.size(); i++) {
                        filenameBytesArr[i] = filenameBytes.get(i);
                    }
                    String filename = new String(filenameBytesArr, StandardCharsets.UTF_8);
                    System.out.println(filename);
                    filenameBytes.clear();
                }
            }
        }

        //Check if the data packet is the last one
        if (dataPacketSize < 512) {
            if (mode == dataMode.RRQ)
                System.out.println("RRQ " + pendingRRQFileName + " complete");

            lastBlockNumberReceived = 0;
            mode = dataMode.UNDIFFERENTIATED;
            pendingRRQFileName = null;
        }

        //Send ACK for the received data packet
        byte[] ackPacket = {0, 4, (byte) (blockNumber >> 8), (byte) blockNumber};
        sendToServer(ackPacket);
    }

    private void handleAckPacket(byte[] packet) {
        short blockNumber = (short) ((packet[2] << 8) | (packet[3] & 0xFF));
        System.out.println("ACK " + blockNumber);

        //Handle the WRQ ACK packets
        if (mode == dataMode.WRQ) {
            if (blockNumber == 0) {
                lastBlockNumberReceived = blockNumber;
            }
            if (blockNumber != lastBlockNumberReceived) {
                //System.out.println("Invalid block number: " + blockNumber);
                pendingWRQFileName = null;
                mode = dataMode.UNDIFFERENTIATED;
            }

            File fileToUpload = new File(pendingWRQFileName);
            if (!fileToUpload.exists()) {
                System.out.println("File does not exist");
                mode = dataMode.UNDIFFERENTIATED;
                pendingWRQFileName = null;
                return; // Don't proceed with the WRQ operation
            }

            try (FileInputStream fileInputStream = new FileInputStream(pendingWRQFileName)) {
                byte[] buffer = new byte[512];
                int bytesRead;
                int currentBlockNumber = 1;

                //Read the file and send the data packets to the server
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    byte[] dataPacket = new byte[bytesRead + 6];
                    //Set opcode for DATA packet
                    dataPacket[0] = 0;
                    dataPacket[1] = 3;

                    //Set packet size
                    dataPacket[2] = (byte) ((bytesRead >> 8) & 0xFF); // Most significant byte
                    dataPacket[3] = (byte) (bytesRead & 0xFF);        // Least significant byte

                    //Set block number
                    dataPacket[4] = (byte) ((currentBlockNumber >> 8) & 0xFF); // Most significant byte
                    dataPacket[5] = (byte) (currentBlockNumber & 0xFF);        // Least significant byte

                    //Send the data packet to the server
                    System.arraycopy(buffer, 0, dataPacket, 6, bytesRead);
                    sendToServer(dataPacket);
                    currentBlockNumber++;

                }
                // Print completion message
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mode = dataMode.UNDIFFERENTIATED;
                pendingWRQFileName = null;
                lastBlockNumberReceived = 0;
            }
        }
    }

    private void handleBcastPacket(byte[] packet) {
        if (WRQiNProcess != null) {
            System.out.println("WRQ " + WRQiNProcess + " complete");
            WRQiNProcess = null;
        }

        //Extract the operation and filename from the packet
        short deletedOrAdded = (short)(packet[2]);

        //Print the BCAST message
        String operation = ((deletedOrAdded == 0) ? "del " : "add ");
        String filename = new String(packet, 3, packet.length - 4, StandardCharsets.UTF_8);
        System.out.println("BCAST " + operation + filename);
    }

    private void handleErrorPacket(byte[] packet) {
        short errorCode = (short)((packet[2] << 8) | (packet[3] & 0xFF));

        //Handle error during RRQ
        if (mode == dataMode.RRQ) {
            if (pendingRRQFileName != null) {
                File fileToDelete = new File(pendingRRQFileName);
                pendingRRQFileName = null;
                fileToDelete.delete();
            }
            pendingRRQFileName = null;
            mode = dataMode.UNDIFFERENTIATED;
        }

        //Reset the mode
        else if (mode != dataMode.UNDIFFERENTIATED) {
            pendingRRQFileName = null;
            mode = dataMode.UNDIFFERENTIATED;
        }

        //Extract the error message to print from the packet
        String errorMessage = new String(packet, 4, packet.length - 5, StandardCharsets.UTF_8);
        System.out.println("ERROR " + errorCode + " " + errorMessage);
    }

    private void sendToServer(byte[] message) {
        //Send the message to the server
        try {
            outputStream.write(message);
            outputStream.flush();
        } catch (IOException e) {e.printStackTrace();}
    }

}





