package lib.core;

import lib.impl.SocketChannelAdapter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {
    private String key = UUID.randomUUID().toString();
    private SocketChannel channel;
    private Receiver receiver;
    private Sender sender;

    public void setup(SocketChannel channel) throws IOException {
        this.channel = channel;
        IoContext ioContext = IoContext.get();
        SocketChannelAdapter socketChannelAdapter = new SocketChannelAdapter(channel, ioContext.getIoProvider(), this);
        this.receiver = socketChannelAdapter;
        this.sender = socketChannelAdapter;

        readNextMessage();
    }

    private void readNextMessage() {
        if (this.receiver != null) {
            try {
                this.receiver.receiveAsync(echoIoEventListener);
            } catch (IOException e) {
                System.out.println("接收数据异常: " + e.getMessage());
            }
        }
    }

    private IoArgs.IoArgsEventListener echoIoEventListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

        }

        @Override
        public void onCompleted(IoArgs args) {
            onReceiveMessage(args.bufferToString());
            readNextMessage();
        }
    };

    protected void onReceiveMessage(String str) {
        System.out.println(key + ":" + str);
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }
}
