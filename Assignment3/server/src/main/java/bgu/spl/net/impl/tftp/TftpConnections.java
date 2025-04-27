package bgu.spl.net.impl.tftp;
import java.util.concurrent.ConcurrentHashMap;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class TftpConnections<T> implements Connections<T> {

    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> connectionsHolder = new ConcurrentHashMap<>();

    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler) {
        connectionsHolder.put(connectionId, handler);
    }

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> addressee = connectionsHolder.get(connectionId);
        if (addressee == null)
            return false;

        addressee.send(msg);
        return true;
    }

    @Override
    public void disconnect(int connectionId) {
        Holder.ids_login.remove(connectionId);
        try {
            this.connectionsHolder.remove(connectionId);
        }catch (Exception ignored){
            this.connectionsHolder.remove(connectionId);
        }
    }

}