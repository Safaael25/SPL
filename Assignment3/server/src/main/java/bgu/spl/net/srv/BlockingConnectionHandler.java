package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.BidiMessagingProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private final Connections<T> connections;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    private final int connectionId;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol,
                                     Connections<T> connections, int connectionId) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.connections = connections;
        this.connectionId = connectionId;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) {
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            protocol.start(connectionId, connections);
            connections.connect(connectionId, this);

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    protocol.process(nextMessage);
                }
            }
            connections.disconnect(connectionId);
        } catch (IOException ex) {
            connections.disconnect(connectionId);
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        connections.disconnect(connectionId);
        sock.close();
    }

    @Override
    public void send(T msg) {
        if (msg != null) {
            try {
                if (out != null ) {
                    if (!protocol.shouldTerminate() && connected) {
                        out.write(encdec.encode(msg));
                        out.flush();
                    }
                }
            } catch (IOException e) {
                connections.disconnect(connectionId);
                e.printStackTrace();
            }
        }
    }
}
