package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.util.Arrays;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private final byte[] bytes = new byte[1 << 10]; // Start with 1024 bytes buffer size
    private int length = 0;
    /**Object to handle opcode decoding*/
    private final Opcode opcode = new Opcode();

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        bytes[length++] = nextByte; // Store the incoming byte

        // Once we have at least 2 bytes, update the packet type
        if (length >= 2 && (opcode.getPacketType() == Opcode.PacketType.Undefined_Opcode)) {
            updatePacketType(); // Update the packet type
        }

        // Check the packet type and decode accordingly
        switch (opcode.getPacketType()) {
            case Undefined_Opcode:
                break;

            case Read_Request: case Write_Request: case Login_Request: case Delete_File_Request: case Broadcast_File:
                if (nextByte == 0 && length > 2) {
                    int packetLength = length;
                    resetBuffer();
                    return Arrays.copyOfRange(bytes, 0, packetLength);
                }
                break;

            case Data_Packet:
                if (length >= 5) {
                    short packetSize = (short) ((bytes[2] << 8) | (bytes[3] & 0xFF));
                    if (length == packetSize + 6) {
                        resetBuffer();
                        return Arrays.copyOfRange(bytes, 0, packetSize + 6);
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
        return null; // Return null if packet decoding is not complete
    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
    }

    private void updatePacketType() {
        /** Combine the first two bytes into a short value to determine the packet type**/
        short typeValue = (short) ((bytes[0] << 8) | (bytes[1] & 0xFF));
        opcode.updatePacketType(typeValue);
    }

    private void resetBuffer() {
        length = 0; // Reset the buffer length for next message
        opcode.resetPacketType();
    }
}
