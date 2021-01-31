package lib.core;

import java.io.Closeable;
import java.io.IOException;

public class IoContext implements Closeable {
    private static IoContext INSTANCE;
    private final IoProvider ioProvider;

    public IoContext start(IoProvider ioProvider) {
        return new StartedBoot(ioProvider).start();
    }

    public IoContext(IoProvider ioProvider) {
        this.ioProvider = ioProvider;
    }

    public IoProvider getIoProvider() {
        return ioProvider;
    }

    @Override
    public void close() throws IOException {
        this.ioProvider.close();
    }

    public static class StartedBoot {
        private IoProvider ioProvider;

        public StartedBoot(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
        }

        public IoContext start() {
            INSTANCE = new IoContext(ioProvider);
            return INSTANCE;
        }
    }
}
