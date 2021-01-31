package lib.core;

import java.io.IOException;

public interface Sender {
    boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException;
}
