import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Message {
    private String content;
    private String receiver;
    private String sender;
    private String command;

    public String getReceiver() {
        return this.receiver;
    }
    public String getContent() {
        return this.content;
    }
    public String getCommand() { return this.command; }

    public void setSender(String sender) {
        this.sender = sender;
    }
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public Message(String content, String receiver, String command) {
        this.content = content;
        this.receiver = receiver;
        this.command = command;
    }

    private static String BuildCurrentTime() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+1"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();

        return "[" + dateFormat.format(date) + "] ";
    }

    public void FormatPingReply() {
        this.content = BuildCurrentTime() + "Server says: " + content + "\n";
    }

    public void FormatMessage() {
        this.content = BuildCurrentTime() + sender + " says: " + content + "\n";
    }
}
