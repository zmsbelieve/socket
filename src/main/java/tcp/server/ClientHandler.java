package tcp.server;

import tcp.utils.CloseUtils;

import java.io.*;
import java.net.Socket;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class ClientHandler {
    private Socket socket;
    private ReadHandler readHandler;
    private WriteHandler writeHandler;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.readHandler = new ReadHandler(socket.getInputStream());
        this.writeHandler = new WriteHandler(socket.getOutputStream());
    }

    public void readToPrint() {
        readHandler.start();
    }

    public void send(String message) {
        writeHandler.send(message);
    }

    class WriteHandler {
        private OutputStream outputStream;
        public WriteHandler(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        private void send(String message) {

        }

        class WriteRunnable implements Runnable {
            @Override
            public void run() {

            }
        }

    }

    class ReadHandler extends Thread {
        private InputStream inputStream;
        private volatile boolean done;

        public ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            while (!done) {
                try {
                    String str = bufferedReader.readLine();
                    System.out.println(str);
                } catch (IOException e) {
                    System.out.println("服务器读数据失败。。。");
                    e.printStackTrace();
                } finally {
                    close();
                }
            }

        }

        private void close() {
            done = true;
            CloseUtils.close(inputStream);
        }
    }
}
