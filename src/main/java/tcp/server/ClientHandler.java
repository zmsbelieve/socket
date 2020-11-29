package tcp.server;

import tcp.utils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class ClientHandler {
    private Socket socket;
    private ReadHandler readHandler;
    private WriteHandler writeHandler;
    private ClientHandlerCallback clientHandlerCallback;

    public ClientHandler(Socket socket, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socket = socket;
        this.readHandler = new ReadHandler(socket.getInputStream());
        this.writeHandler = new WriteHandler(socket.getOutputStream());
        this.clientHandlerCallback = clientHandlerCallback;
    }

    public void readToPrint() {
        readHandler.start();
    }

    public void send(String message) {
        writeHandler.send(message);
    }

    public void exit() {
        readHandler.close();
        writeHandler.exit();
        CloseUtils.close(socket);
        System.out.println("客户端已经退出");
    }

    class WriteHandler {
        private PrintStream printStream;
        ExecutorService executorService;
        private boolean done;

        public WriteHandler(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        private void send(String message) {
            executorService.execute(new WriteRunnable(message));
        }

        private void exit() {
            done = true;
            CloseUtils.close(printStream);
            executorService.shutdownNow();
        }

        class WriteRunnable implements Runnable {
            private String message;

            public WriteRunnable(String message) {
                this.message = message;
            }

            @Override
            public void run() {
                if (WriteHandler.this.done) {
                    return;
                }
                WriteHandler.this.printStream.println(message);
            }
        }

    }

    public interface ClientHandlerCallback {
        // 自身关闭通知
        void onSelfClosed(ClientHandler handler);

        // 收到消息时通知
        void onNewMessageArrived(ClientHandler handler, String msg);
    }


    class ReadHandler extends Thread {
        private InputStream inputStream;
        private volatile boolean done;

        public ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                while (!done) {
                    String str = bufferedReader.readLine();
                    System.out.println(str);
                    clientHandlerCallback.onNewMessageArrived(ClientHandler.this,str );
                }
            } catch (Exception e) {
                close();
            }

        }

        private void close() {
            done = true;
            CloseUtils.close(inputStream);
            clientHandlerCallback.onSelfClosed(ClientHandler.this);
        }
    }
}
