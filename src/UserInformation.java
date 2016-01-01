import com.google.gson.annotations.SerializedName;

import java.io.OutputStream;
import java.io.Serializable;

public class UserInformation implements Serializable {
    private int id;
    private String username;
    @SerializedName("email_address")
    private String emailAddress;
    private String avatar;
    private transient String ip;
    private transient int port;
    public transient OutputStream outputStream = null;

    public UserInformation () { }

    public int getId() { return this.id; }
    public String getUsername() { return this.username; }
    public String getIP() { return this.ip; }
    public int getPort() {
        return this.port;
    }
    public String getEmail() { return this.emailAddress; }
    public OutputStream getOutputStream() {
        return this.outputStream;
    }
    public String getAvatar() { return this.avatar; }

    public void setIP(String IP) {
        this.ip = IP;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setOutputStream(OutputStream stream) {
        this.outputStream = stream;
    }
    public void setAvatar(String avatarUrl) {
        this.avatar = avatarUrl;
    }
}
