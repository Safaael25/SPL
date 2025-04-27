package bgu.spl.net.impl.tftp;

//Similar to the Opcode class implemented to the server side, this class is used to determine the type of packet received
public class Opcode {
    public enum PacketType {
        Undefined_Opcode,
        Read_Request,
        Write_Request,
        Data_Packet,
        Acknowledgment,
        Error,
        Directory_Listing_Request,
        Login_Request,
        Delete_File_Request,
        Broadcast_File,
        Disconnect
    }

    private PacketType packetType;

    public Opcode() {
        resetPacketType();
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public void updatePacketType(short typeValue) {
        // Determine the packet type based on the combined value
        switch (typeValue) {
            case 1:
                packetType = PacketType.Read_Request;
                break;
            case 2:
                packetType = PacketType.Write_Request;
                break;
            case 3:
                packetType = PacketType.Data_Packet;
                break;
            case 4:
                packetType = PacketType.Acknowledgment;
                break;
            case 5:
                packetType = PacketType.Error;
                break;
            case 6:
                packetType = PacketType.Directory_Listing_Request;
                break;
            case 7:
                packetType = PacketType.Login_Request;
                break;
            case 8:
                packetType = PacketType.Delete_File_Request;
                break;
            case 9:
                packetType = PacketType.Broadcast_File;
                break;
            case 10:
                packetType = PacketType.Disconnect;
                break;
            default:
                packetType = PacketType.Undefined_Opcode;
                break;
        }
    }

    public void resetPacketType() {
        packetType = PacketType.Undefined_Opcode;
    }
}
