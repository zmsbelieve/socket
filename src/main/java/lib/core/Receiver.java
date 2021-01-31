package lib.core;

import java.io.IOException;

public interface Receiver {
    boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException;
}
