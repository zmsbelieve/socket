package tcp.server;

import tcp.constants.TCPConstants;
import tcp.constants.UDPConstants;
import tcp.utils.ByteUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class UDPProvider {
    public static Provider PROVIDER = null;

    public static void start(int port){
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn, port);
        provider.start();
        PROVIDER = provider;
    }
    private static void stop() {
        if (PROVIDER != null) {
            PROVIDER.exit();
            PROVIDER = null;
        }
    }

    static class Provider extends Thread {
        private int port;
        private volatile boolean done = false;
        private byte[] bytes = new byte[128];
        DatagramSocket ds = null;
        private String sn;
        private int minLen = UDPConstants.HEADER.length + 2 + 4;

        public Provider(String sn, int port) {
            this.port = port;
            this.sn = sn;
        }

        @Override
        public void run() {
            System.out.println("UDPProvider  start...");
            try {
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);
                while (!done) {
                    DatagramPacket receivePacket = new DatagramPacket(bytes, bytes.length);
                    ds.receive(receivePacket);
                    String ip = receivePacket.getAddress().getHostAddress();
                    int dataLength = receivePacket.getLength();
                    int receivePort = receivePacket.getPort();
                    System.out.println("收到来自客户端 " + ip + ":" + receivePort + "数据长度为:" + dataLength + "的udp包");
                    byte[] receiveData = receivePacket.getData();
                    boolean isValid = dataLength >= minLen && ByteUtils.startsWith(receiveData, UDPConstants.HEADER);
                    if (!isValid) {
                        System.out.println("非法请求数据");
                        continue;
                    }
                    int index = UDPConstants.HEADER.length;
                    short cmd = (short) (receiveData[index++] << 8 | receiveData[index++] & 0xff);
                    int responsePort = receiveData[index++] << 24 | (receiveData[index++] & 0xff << 16)
                            | (receiveData[index++] & 0xff) << 8 | receiveData[index++] & 0xff;
                    if (cmd != 1 || responsePort <= 0) {
                        continue;
                    }
                    ByteBuffer responseBuffer = ByteBuffer.allocate(128);
                    responseBuffer.put(UDPConstants.HEADER);
                    responseBuffer.putShort((short) 2);
                    responseBuffer.putInt(port);
                    responseBuffer.put(sn.getBytes());
                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer.array(), responseBuffer.position() + 1);
                    responsePacket.setPort(responsePort);
                    responsePacket.setAddress(receivePacket.getAddress());
                    DatagramSocket responseSocket = new DatagramSocket();
                    responseSocket.send(responsePacket);
//                    responseSocket.close();//send后close()不知道为什么没发送出去
                    System.out.println("UDPProvider response to:" + ip + "\tport:" + responsePort + "\tdataLen:" + (responseBuffer.position() + 1));
                }
            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                exit();
            }
        }

        private void exit() {
            done = true;
            if (ds != null) {
                ds.close();
            }
        }
    }
}
