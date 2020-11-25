package tcp.client;

import tcp.client.bean.ServerInfo;
import tcp.constants.UDPConstants;
import tcp.utils.ByteUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class UDPSearch {

    public static ServerInfo search(int timeout) throws IOException, InterruptedException {
        CountDownLatch receive = new CountDownLatch(1);
        Listener listener = Listener.listen(receive);
        sendBroadcast();
        if (listener == null) {
            return null;
        }
        receive.await(timeout, TimeUnit.SECONDS);
        ServerInfo serverInfo = listener.getAndClose();
        System.out.println(serverInfo);
        return null;
    }

    private static void sendBroadcast() throws IOException {
        System.out.println("UDPSearch start...");

        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        byteBuffer.put(UDPConstants.HEADER);
        byteBuffer.putShort((short) 1);
        byteBuffer.putInt(UDPConstants.PORT_CLIENT_RESPONSE);
        DatagramPacket datagramPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);
        datagramPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        datagramPacket.setPort(UDPConstants.PORT_SERVER);
        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.send(datagramPacket);
//        datagramSocket.close();//send后close()不知道为什么没发送出去

        System.out.println("UDPSearch finished ...");
    }

    static class Listener extends Thread {
        private CountDownLatch start;
        private CountDownLatch receive;
        private volatile boolean done = false;
        private byte[] bytes = new byte[128];
        DatagramSocket ds = null;
        int minLen = UDPConstants.HEADER.length + 2 + 4;
        private List<ServerInfo> serverInfoList = new ArrayList<>();

        public Listener(CountDownLatch start, CountDownLatch receive) {
            this.start = start;
            this.receive = receive;
        }

        public static Listener listen(CountDownLatch receive) throws InterruptedException {
            CountDownLatch start = new CountDownLatch(1);
            Listener listener = new Listener(start, receive);
            listener.start();
            start.await();
            return listener;
        }

        @Override
        public void run() {

            start.countDown();
            System.out.println("UDPListener start...");
            try {
                ds = new DatagramSocket(UDPConstants.PORT_CLIENT_RESPONSE);
                while (!done) {
                    DatagramPacket receivePacket = new DatagramPacket(bytes, bytes.length);
                    ds.receive(receivePacket);
                    byte[] receBytes = receivePacket.getData();
                    boolean isValid = receBytes.length >= minLen && ByteUtils.startsWith(receBytes, UDPConstants.HEADER);
                    if (!isValid) {
                        continue;
                    }
                    String ip = receivePacket.getAddress().getHostAddress();
                    int port = receivePacket.getPort();
                    int dataLength = receivePacket.getLength();
                    System.out.println("收到来自 " + ip + ":" + port + "共" + dataLength + "长度");
                    ByteBuffer receiveBuffer = ByteBuffer.wrap(receBytes, UDPConstants.HEADER.length, dataLength);
                    short cmd = receiveBuffer.getShort();
                    int tcpPort = receiveBuffer.getInt();
                    if (cmd != 2 || tcpPort <= 0) {
                        System.out.println("UDPSearcher receive cmd:" + cmd + "\tserverPort:" + tcpPort);
                        continue;
                    }
                    ServerInfo serverInfo = new ServerInfo();
                    serverInfo.setAddress(ip);
                    serverInfo.setPort(tcpPort);
                    serverInfo.setSn(new String(receBytes, minLen, dataLength - minLen));
                    serverInfoList.add(serverInfo);
                    receive.countDown();
                }

            } catch (IOException e) {

            } finally {
                close();
            }
        }

        private void close() {
            done = true;
            if (ds != null) {
                ds.close();
            }
        }

        public ServerInfo getAndClose() {
            close();
            if (!serverInfoList.isEmpty()) {
                return serverInfoList.get(0);
            }
            return null;
        }
    }
}
