package xyz.thedevspot.models;
import com.google.gson.annotations.SerializedName;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;

public class UserInformation implements Serializable {
    private int id;
    private String username;
    @SerializedName("email_address")
    private String emailAddress;
    private String avatar;
    private transient InetAddress ip;
    private transient int tcpPort;
    private transient int udpPort;
    public transient OutputStream outputStream = null;

    public UserInformation () { }

    public int getId() { return this.id; }
    public String getUsername() { return this.username; }
    public InetAddress getIP() { return this.ip; }
    public int getTcpPort() {
        return this.tcpPort;
    }
    public int getUdpPort() {
        return this.udpPort;
    }
    public String getEmail() { return this.emailAddress; }
    public OutputStream getOutputStream() {
        return this.outputStream;
    }
    public String getAvatar() { return this.avatar; }

    public void setIP(InetAddress IP) {
        this.ip = IP;
    }
    public void setTcpPort(int port) {
        this.tcpPort = port;
    }
    public void setUdpPort(int port) {
        this.udpPort = port;
    }
    public void setOutputStream(OutputStream stream) {
        this.outputStream = stream;
    }
    public void setAvatar(String avatarUrl) {
        this.avatar = avatarUrl;
    }
}
