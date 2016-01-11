package xyz.thedevspot;

import xyz.thedevspot.helpers.CallManager;
import xyz.thedevspot.models.UserInformation;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UdpListener extends Thread {
    private DatagramSocket udpSocket;
    private UserInformation receiver;
    private UserInformation sender;
    private boolean isSocketOpen;
    private boolean isAddressMapped;
    private static Thread listenerThread;

    public UdpListener(int portNumber, UserInformation receiver, UserInformation sender) {
        try {
            this.udpSocket = new DatagramSocket(null);
            this.udpSocket.setReuseAddress(true);
            this.udpSocket.bind(new InetSocketAddress(portNumber));

            this.receiver = receiver;
            this.sender = sender;
            this.isSocketOpen = true;
            this.isAddressMapped = false;
        } catch (SocketException e) {
            e.printStackTrace();
            this.isSocketOpen = false;
        }
    }

    public void startListening() {
        if (!this.isSocketOpen) return;

        listenerThread = this;
        listenerThread.start();
    }

    public void run() {
        this.processUdpPackage();
    }

    private String createConnectionIdentifier() {
        return UUID.randomUUID().toString();
    }

    private DatagramPacket createResponsePacket(UserInformation user, String id) {
        byte[] responseData = id.getBytes(StandardCharsets.UTF_8);
        InetAddress ipAddress = user.getIP();

        return new DatagramPacket(responseData, responseData.length, ipAddress, user.getUdpPort());
    }

    private void mapClientInfo(DatagramPacket packet) {
        if (this.receiver.getUdpPort() == 0 || this.sender.getUdpPort() == 0) {
            this.isAddressMapped = false;

            String username = new String(packet.getData(), packet.getOffset(), packet.getLength());
            username = username.trim();

            if (username.equals(receiver.getUsername())) {
                System.out.println("Mapping receiver at: " + "IP " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
                receiver.setUdpPort(packet.getPort());
            }
            if (username.equals(sender.getUsername())) {
                System.out.println("Mapping sender at: " + "IP " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
                sender.setUdpPort(packet.getPort());
            }

            if (this.receiver.getUdpPort() == 0 || this.sender.getUdpPort() == 0) return;
        }

        String connectionId = createConnectionIdentifier();
        // Store information about the connection into the CallManager class in order to retrieve the appropriate thread later on
        CallManager.storeCallInfo(connectionId, this);

        DatagramPacket senderResponse = createResponsePacket(this.sender, connectionId);
        DatagramPacket receiverResponse = createResponsePacket(this.receiver, connectionId);

        try {
            this.udpSocket.send(senderResponse);
            this.udpSocket.send(receiverResponse);
        } catch (IOException e) {
            e.printStackTrace();

            this.isAddressMapped = false;
            return;
        }

        this.isAddressMapped = true;
    }

    private void processUdpPackage() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (this.isSocketOpen) {

                try {
                    if (!this.isSocketOpen) break;

                    this.udpSocket.receive(packet);

                    if (!this.isAddressMapped) {
                        mapClientInfo(packet);
                        continue;
                    }

                    byte[] responseData = packet.getData();
                    InetAddress ipAddress = null;

                    if (responseData == null) {
                        this.isSocketOpen = false;
                        break;
                    }

                    try {
                        ipAddress = packet.getAddress();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (ipAddress != null) {
                        DatagramPacket response = null;

                        String packetIP = ipAddress.getHostAddress();
                        String senderIP = this.sender.getIP().getHostAddress();
                        String receiverIP = this.receiver.getIP().getHostAddress();

                        if (packetIP.equals(senderIP) && packet.getPort() == sender.getUdpPort()) {
                            response = new DatagramPacket(responseData, responseData.length, receiver.getIP(), receiver.getUdpPort());
                        }
                        else if (packetIP.equals(receiverIP) && packet.getPort() == receiver.getUdpPort()) {
                            response = new DatagramPacket(responseData, responseData.length, sender.getIP(), sender.getUdpPort());
                        }

                        if (response != null) this.udpSocket.send(response);
                    }

                    if (!this.isSocketOpen) break;
                } catch (IOException e) {
                    this.close();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.close();
        }
        finally {
            if (this.udpSocket != null) {
                this.udpSocket.close();
                this.udpSocket = null;
            }
        }
    }

    public void close() {
        this.receiver.setUdpPort(0);
        this.sender.setUdpPort(0);
        this.isSocketOpen = false;
        this.isAddressMapped = false;
    }
}
