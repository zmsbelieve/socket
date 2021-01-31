package lib.core;

import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connector {
    private String key = UUID.randomUUID().toString();
    private SocketChannel channel;
    private Receiver receiver;
    private Sender sender;

    public void setup(SocketChannel channel){
        this.channel = channel;
    }
}
