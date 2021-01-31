package tcp.client.bean;

/**
 * @author zhangms
 * @date 2020/11/25
 */
public class ServerInfo {

    private String sn;
    private int port;
    private String address;

    public String getSn() {
        return sn;
    }

    public ServerInfo setSn(String sn) {
        this.sn = sn;
        return this;
    }

    public int getPort() {
        return port;
    }

    public ServerInfo setPort(int port) {
        this.port = port;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public ServerInfo setAddress(String address) {
        this.address = address;
        return this;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "sn='" + sn + '\'' +
                ", port=" + port +
                ", address='" + address + '\'' +
                '}';
    }
}
