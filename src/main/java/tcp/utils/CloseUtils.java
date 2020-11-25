package tcp.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class CloseUtils {

    public static void close(Closeable... closeables) {
        if (closeables == null) {
            return;
        }

        try {
            for (Closeable closeable : closeables) {
                closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
