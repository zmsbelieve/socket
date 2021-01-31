package lib.impl;

import lib.core.IoArgs;
import lib.core.IoProvider;
import lib.core.Receiver;
import lib.core.Sender;
import tcp.utils.CloseUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements Receiver, Sender, Closeable {
    private AtomicBoolean isClosed = new AtomicBoolean(false);
    private SocketChannel channel;
    private IoProvider ioProvider;
    private OnChannelStatusChangedListener changedListener;

    private IoArgs.IoArgsEventListener receiverListener;
    private IoArgs.IoArgsEventListener senderListener;
    //注册轮询到,然后开始接受
    private final IoProvider.HandleInputCallback inputCallback = new IoProvider.HandleInputCallback() {
        @Override
        public void canProviderInput() {
            if (isClosed.get()) {
                return;
            }
            IoArgs args = new IoArgs();
            if (receiverListener != null) {
                //回调
                receiverListener.onStarted(args);
            }
            try {
                if (args.read(channel) > 0 && receiverListener != null) {
                    //回调
                    receiverListener.onCompleted(args);
                } else {
                    throw new IOException("cannot read any data");
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IoProvider.HandleOutputCallback outputCallback = new IoProvider.HandleOutputCallback() {
        @Override
        public void canProviderOutput(Object attach) {
            if (isClosed.get()) {
                return;
            }
            //TODO:
            senderListener.onCompleted(null);
        }
    };

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangedListener changedListener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.changedListener = changedListener;
        //设置非阻塞
        channel.configureBlocking(false);
    }

    @Override
    public void close() throws IOException {
        if (!isClosed.compareAndSet(false, true)) {
            throw new IOException("current is closed");
        }
        ioProvider.unRegisterInput(channel);
        ioProvider.unRegisterOutput(channel);
        CloseUtils.close(channel);
        //回调
        changedListener.onChannelClosed(channel);
    }

    @Override
    public boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("current is closed");
        }
        receiverListener = listener;
        return ioProvider.registerInput(channel, inputCallback);
    }

    @Override
    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("current is closed");
        }
        senderListener = listener;
        outputCallback.setAttach(args);
        return ioProvider.registerOutput(channel, outputCallback);
    }

    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}
