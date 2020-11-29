package tcp.client;

import tcp.client.bean.ServerInfo;

import java.io.*;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class Client {

    public static void main(String[] args) throws IOException, InterruptedException {
        //UDP搜索tcp连接信息
        ServerInfo serverInfo = UDPSearch.search(5);
        //tcp连接
        if (serverInfo != null) {
            TCPClient tcpClient = TCPClient.startWith(serverInfo);
            if (tcpClient == null) {
                return;
            }
            write(tcpClient);
        }
    }

    public static void write(TCPClient tcpClient) {
        InputStream in = System.in;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
        while (true) {
            try {
                String str = bufferedReader.readLine();
                if ("bye".equals(str)) {
                    break;
                }
                tcpClient.send(str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        tcpClient.stop();
    }
}
