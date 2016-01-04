package xyz.thedevspot;

import com.google.gson.Gson;
import xyz.thedevspot.helpers.CallManager;
import xyz.thedevspot.helpers.ClientsManager;
import xyz.thedevspot.models.BaseResponse;
import xyz.thedevspot.models.Message;
import xyz.thedevspot.models.UserInformation;
import xyz.thedevspot.network.WebConnector;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ServerSocketListener extends Thread {
    private final static int portNumber = 9999;
    private UserInformation client = null;
    private Socket clientSocket = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private Thread clientThread = null;
    private Thread authorizationThread = null;
    private Thread notificationThread = null;

    // Listener for all of the incoming calls
    private UdpListener udpListener;

    private enum MessageType {
        Message, Ping, Request
    }

    private enum ClientState {
        NotAuthorized, Authorized
    }
    private ClientState isAuthorized;

    public ServerSocketListener(Socket client) {
        this.clientSocket = client;
        this.clientThread = new Thread(this);
        this.authorizationThread = new Thread(this);
        this.isAuthorized = ClientState.NotAuthorized;

        try {
            this.openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        switch (this.isAuthorized) {
            case NotAuthorized:
                authorize();
                break;
            case Authorized:
                getResponse();
                break;
        }
    }

    private void startServer() { this.clientThread.start(); }

    private void askForAuthorization() { this.authorizationThread.run(); }

    private void notifyConnectedClients(boolean clientDisconnected) {
        this.notificationThread = new Thread(() -> notify(clientDisconnected));
        this.notificationThread.start();
    }

    private void notify(boolean isDisconnectNotification) {
        ArrayList<UserInformation> contacts = WebConnector.getClientContacts(this.client.getId());
        String command;

        if (isDisconnectNotification) command = "/offline";
        else command = "/online";

        if (contacts == null) return;

        for (UserInformation contact: contacts) {
            UserInformation availableClient = ClientsManager.getClientInfo(contact.getUsername());

            if (availableClient != null) {
                Gson gson = new Gson();
                String jsonMessage;
                PrintWriter writer = new PrintWriter(availableClient.getOutputStream(), true);

                Message message = new Message(client.getUsername(), availableClient.getUsername(), command);
                jsonMessage = gson.toJson(message);
                writer.println(jsonMessage + "\n");

                if (!isDisconnectNotification) {
                    writer = new PrintWriter(client.getOutputStream(), true);
                    message.setContent(availableClient.getUsername());
                    message.setReceiver(client.getUsername());

                    jsonMessage = gson.toJson(message);
                    writer.println(jsonMessage + "\n");
                }
            }
        }

        try {
            this.notificationThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void authorize() {
        boolean closeConnection = false;

        while (this.clientSocket.isConnected()) {
            try {
                PrintWriter writer = new PrintWriter(outputStream, true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String message = reader.readLine();

                if (message == null) {
                    closeConnection = true;
                    break;
                }

                if (message.contains("/disconnect") || message.equals("")) closeConnection = true;
                else {
                    Gson gson = new Gson();
                    System.out.println("Authorization message: " + message);

                    client = WebConnector.tryAuthorizeUser(message);

                    if (client != null) {

                        client.setIP(clientSocket.getInetAddress().toString());
                        client.setPort(clientSocket.getPort());
                        client.setOutputStream(this.outputStream);

                        // Send the account information back to the client
                        BaseResponse response = new BaseResponse(200, client, "");
                        writer.println(gson.toJson(response));

                        // Push clients to the list of currently available clients
                        // Used to determine whether the client is online when a different client performs a check
                        ClientsManager.storeClient(client);
                        this.notifyConnectedClients(false);
                        break;
                    }
                    else {
                        BaseResponse response = new BaseResponse(400, null, "You have entered wrong credentials, or account does not exist.");
                        writer.println(gson.toJson(response));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                closeConnection = true;
            } finally {
                if (closeConnection) {
                    try {
                        this.closeConnection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (closeConnection) break;
        }

        if (client != null) this.isAuthorized = ClientState.Authorized;
    }

    private void sendThroughPipe(Message message, MessageType msgType) {
        Gson gson = new Gson();
        PrintWriter writer = new PrintWriter(outputStream, true);
        String username;
        String jsonMessage;

        if (msgType == MessageType.Ping) message.formatPingReply();
        else message.formatReply();

        jsonMessage = gson.toJson(message);

        // In case that we have received a request from another client, skip sending it back to ourselves
        if (msgType != MessageType.Request) {
            writer.println(jsonMessage);
        }

        username = message.getReceiver();
        UserInformation receiver = ClientsManager.getClientInfo(username);

        // Send all messages besides ping to the receiver - ping is associated only with the sender
        if (receiver != null && msgType != MessageType.Ping) {
            writer = new PrintWriter(receiver.getOutputStream(), true);
            writer.println(jsonMessage);
        }
    }

    private void getResponse() {
        boolean closeConnection = false;

        while (this.clientSocket.isConnected()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                String content = reader.readLine();
                System.out.println("Received content: " + content);

                if (content == null) {
                    closeConnection = true;
                    break;
                }

                if (!content.equals("")) {
                    Gson gson = new Gson();
                    Message message = gson.fromJson(content, Message.class);
                    message.setSender(client.getUsername());

                    if (message.getCommand().equals("/disconnect")) closeConnection = true;
                    else if (message.getCommand().equals("/ping")) {
                        Message serverMessage = new Message("pong!", this.client.getUsername(), "/ping");
                        sendThroughPipe(serverMessage, MessageType.Ping);
                    }
                    else if (message.getCommand().equals("/request")) {
                        Message requestMessage = new Message(message.getContent(), message.getReceiver(), "/request");
                        sendThroughPipe(requestMessage, MessageType.Request);
                    }
                    else if (message.getCommand().equals("/accept")) {
                        Message acceptRequestMessage = new Message(message.getContent(), message.getReceiver(), "/accept");
                        sendThroughPipe(acceptRequestMessage, MessageType.Request);
                    }
                    else if (message.getCommand().equals("/online")) {
                        Message onlineMsg = new Message(message.getContent(), message.getReceiver(), "/online");
                        sendThroughPipe(onlineMsg, MessageType.Request);
                    }
                    else if (message.getCommand().contains("/call")) {
                        if (message.getCommand().contains("/close")) {
                            if (message.getContent() != null) {
                                UdpListener callListener = CallManager.getCallInfo(message.getContent());

                                if (callListener != null) {
                                    callListener.close();
                                    CallManager.clearCallInfo(message.getContent());
                                }
                            }

                            Message closeCallMsg = new Message("", message.getReceiver(), "/call/close");
                            sendThroughPipe(closeCallMsg, MessageType.Request);
                        }
                        else if (message.getCommand().contains("/accept")) {
                            UserInformation receiver = ClientsManager.getClientInfo(message.getReceiver());

                            this.udpListener = new UdpListener(portNumber, receiver);
                            this.udpListener.startListening();

                            // Store information about the connection into the xyz.thedevspot.helpers.CallManager class in order to retrieve the appropriate thread later on
                            CallManager.storeCallInfo(message.getContent(), this.udpListener);

                            Message acceptCallMsg = new Message("", message.getReceiver(), "/call/accept");
                            sendThroughPipe(acceptCallMsg, MessageType.Request);
                        }
                        else if (message.getCommand().equals("/call"))
                        {
                            Message initCallMsg = new Message("", message.getReceiver(), "/call");
                            initCallMsg.setSender(client.getUsername());
                            sendThroughPipe(initCallMsg, MessageType.Request);
                        }
                    }
                    else {
                        sendThroughPipe(message, MessageType.Message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                closeConnection = true;
            } finally {
                if (closeConnection) {
                    System.out.println("Closing connection with socket: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
                    try {
                        this.closeConnection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (closeConnection) break;
        }
    }

    private void openStream() throws IOException {
        this.inputStream = this.clientSocket.getInputStream();
        this.outputStream = this.clientSocket.getOutputStream();
    }

    private void closeConnection() throws IOException {
        Gson gson = new Gson();
        PrintWriter writer = new PrintWriter(outputStream, true);

        // If there's client, it means that he didn't authorize, therefore no need for a graceful disconnect
        // As well as no need to communicate with the other connected clients
        if (this.client != null) {
            Message message = new Message("OK", this.client.getUsername(), "/disconnect");
            String jsonMessage = gson.toJson(message);
            writer.println(jsonMessage);

            this.notifyConnectedClients(true);
            ClientsManager.removeClient(this.client);
        }

        if (this.clientSocket != null) this.clientSocket.close();
        if (this.outputStream != null) this.outputStream.close();
        if (this.inputStream != null) this.inputStream.close();

        // If there is an open connection existing while we disconnect, we need that stopped
        if (this.udpListener != null) {
            if (this.udpListener.isConnectionOpen()) this.udpListener.close();
        }

        System.out.println("Connection was successfully closed.");
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(portNumber);
        System.out.println("Server has been successfully started.\nListening for connection at port: " + serverSocket.getLocalPort());

        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("Client: " + client.getInetAddress() + ":" + client.getPort() + " connected.");
            ServerSocketListener listener = new ServerSocketListener(client);

            try {
                listener.askForAuthorization();

                // If no connection was established, just skip to the loop head again and await for connection
                if (!listener.clientSocket.isConnected() || listener.client == null) continue;

                listener.startServer();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        serverSocket.close();
    }
}