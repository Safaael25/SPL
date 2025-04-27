package bgu.spl.net.impl.tftp;

import java.io.IOException;
import java.net.Socket;

public class TftpClientMain {
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"localhost", "7777"};
        }
        if (args.length == 1) {
            args = new String[]{args[0], "7777"};
        }

        try {
            Socket socket = new Socket("localhost", 7777);/// ** 127 or this?
            TftpClient client = new TftpClient(socket);
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
