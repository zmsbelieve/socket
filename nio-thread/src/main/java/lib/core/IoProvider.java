package lib.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

//注册
public interface IoProvider extends Closeable {

    boolean registerInput(SocketChannel channel, HandleInputCallback handleInputCallback);

    boolean registerOutput(SocketChannel channel, HandleOutputCallback handleOutputCallbak);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);

    abstract class HandleInputCallback implements Runnable {
        @Override
        public void run() {
            canProviderInput();
        }

        public abstract void canProviderInput();
    }

    abstract class HandleOutputCallback implements Runnable {
        private Object attach;

        public void setAttach(Object attach) {
            this.attach = attach;
        }

        @Override
        public void run() {
            canProviderOutput(attach);
        }

        public abstract void canProviderOutput(Object attach);
    }
}
