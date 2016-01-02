package xyz.thedevspot.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Message {
    private String timestamp;
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
    public String getTimestamp() { return this.timestamp; }
    public String getSender() { return this.sender;}

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

    private static String buildCurrentTime() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+1"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();

        return dateFormat.format(date);
    }

    public void formatPingReply() {
        this.content = buildCurrentTime() + "Server says: " + content;
    }

    public void formatReply() {
        this.timestamp = buildCurrentTime();
    }
}
