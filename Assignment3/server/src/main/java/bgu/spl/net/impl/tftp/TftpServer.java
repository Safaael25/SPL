package bgu.spl.net.impl.tftp;
import bgu.spl.net.srv.Server;

public class TftpServer {

    public static void main(String[] args) {
        //If no port is given, use the default port 7777
        if (args.length == 0) {
            String port = "7777";
            args = new String[]{port};
        }

        //Initialize the server
        Server.threadPerClient(
                (Integer.parseInt(args[0])), //port
                () -> new TftpProtocol(), //protocol factory
                TftpEncoderDecoder::new
        ).serve();
    }
}