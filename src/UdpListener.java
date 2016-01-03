import xyz.thedevspot.models.UserInformation;

import java.io.IOException;
import java.net.*;

public class UdpListener extends Thread{
    private DatagramSocket udpSocket;
    private UserInformation receiver;
    private Thread listenerThread;
    private boolean isSocketOpen;

    public UdpListener(int portNumber, UserInformation receiver) {
        try {
            this.udpSocket = new DatagramSocket(null);
            this.udpSocket.setReuseAddress(true);
            this.udpSocket.bind(new InetSocketAddress(portNumber));

            this.receiver = receiver;
            this.isSocketOpen = true;

            this.listenerThread = new Thread(this);
        } catch (SocketException e) {
            e.printStackTrace();
            this.isSocketOpen = false;
        }
    }

    public void startListening() {
        if (!this.isSocketOpen) return;

        this.listenerThread.start();
    }

    public void run() {
        this.processUdpPackage();
    }

    private void processUdpPackage() {
        try {
            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (this.isSocketOpen) {

                try {
                    if (!this.isSocketOpen) break;

                    this.udpSocket.receive(packet);
                    byte[] responseData = packet.getData();
                    InetAddress ipAddress = null;

                    if (responseData == null) {
                        this.isSocketOpen = false;
                        break;
                    }

                    System.out.println("UDP packet received at: " + System.currentTimeMillis());

                    try {
                        ipAddress = InetAddress.getByName("localhost");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (ipAddress != null) {
                        DatagramPacket response = new DatagramPacket(responseData, responseData.length, ipAddress , receiver.getPort());
                        this.udpSocket.send(response);
                    }

                    if (!this.isSocketOpen) break;
                } catch (IOException e) {
                    e.printStackTrace();

                    this.isSocketOpen = false;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (this.udpSocket != null) {
                this.udpSocket.close();
            }
        }
    }

    public void close() {
        this.isSocketOpen = false;
        this.udpSocket.disconnect();
    }

    public boolean isConnectionOpen() {
        return this.udpSocket != null && this.udpSocket.isConnected() && this.isSocketOpen;
    }
}
