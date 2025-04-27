package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.util.Arrays;

//Similar to the TftpEncoderDecoder class, but with a different decodeNextByte method for Broadcast_File handling
public class TftpClientEncDec implements MessageEncoderDecoder<byte[]> {
    private final byte[] bytes = new byte[1 << 10]; // Start with 1024
    private int length = 0;
    private final Opcode opcode = new Opcode(); // Create an instance of the Opcode class

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        bytes[length++] = nextByte; //Store the incoming byte

        if (length >= 2 && (opcode.getPacketType() == Opcode.PacketType.Undefined_Opcode)) {
            updatePacketType(); //Update the packet type once we have at least 2 bytes
        }

        //Check for each packet type and decode accordingly
        switch (opcode.getPacketType()) {
            case Undefined_Opcode:
                break;

            case Broadcast_File:
                if (nextByte == 0 && length > 3) {
                    int packetLength = length;
                    resetBuffer();
                    return Arrays.copyOfRange(bytes, 0, packetLength);
                }
                break;

            case Read_Request: case Write_Request: case Login_Request: case Delete_File_Request:
                if (nextByte == 0 && length > 2) {
                    int packetLength = length;
                    resetBuffer();
                    return Arrays.copyOfRange(bytes, 0, packetLength);
                }
                break;

            case Data_Packet:
                if (length >= 5) {
                    short packetSize = (short) ((bytes[2] << 8) | (bytes[3] & 0xFF));
                    if (length == packetSize+6) {
                        resetBuffer();
                        return Arrays.copyOfRange(bytes, 0, packetSize+6);
                    }
                }
                break;

            case Acknowledgment:
                if (length >= 4) {
                    resetBuffer();
                    return Arrays.copyOfRange(bytes, 0, 4);
                }
                break;

            case Error:
                if (nextByte == 0 && length > 4) {
                    int packetLength = length;
                    resetBuffer();
                    return Arrays.copyOfRange(bytes, 0, packetLength);
                }
                break;

            case Directory_Listing_Request: case Disconnect:
                if (length >= 2) {
                    resetBuffer();
                    return Arrays.copyOfRange(bytes, 0, 2);
                }
                break;
        }

        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        return (message);
    }

    private void updatePacketType() {
        //Combine the first two bytes into a short value
        short typeValue = (short) ((bytes[0] << 8) | (bytes[1] & 0xFF));
        opcode.updatePacketType(typeValue); // Update the packet type
    }

    private void resetBuffer() {
        length = 0;
        opcode.resetPacketType();
    }
}