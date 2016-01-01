import com.google.gson.Gson;
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
    private UserInformation clientUser = null;
    private Socket clientSocket = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private Thread clientThread = null;
    private Thread authorizationThread = null;
    private Thread notificationThread = null;

    public enum MessageType {
        Message, Ping, Request
    }

    public enum ClientState {
        NotAuthorized, Authorized
    }
    private ClientState isAuthorized;

    public ServerSocketListener(Socket client) {
        this.clientSocket = client;
        this.clientThread = new Thread(this);
        this.authorizationThread = new Thread(this);
        this.isAuthorized = ClientState.NotAuthorized;

        try {
            this.OpenStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        switch (this.isAuthorized) {
            case NotAuthorized:
                Authorize();
                break;
            case Authorized:
                GetResponse();
                break;
        }
    }

    private void StartServer() { this.clientThread.start(); }

    private void AskForAuthorization() { this.authorizationThread.run(); }

    private void NotifyConnectedClients(boolean clientDisconnected) {
        this.notificationThread = new Thread(() -> Notify(clientDisconnected));
        this.notificationThread.start();
    }

    private void Notify(boolean isDisconnectNotification) {
        ArrayList<UserInformation> contacts = WebConnector.GetClientContacts(this.clientUser.getId());
        String command;

        if (isDisconnectNotification) command = "/offline";
        else command = "/online";

        if (contacts == null) return;

        for (UserInformation contact: contacts) {
            UserInformation availableClient = AvailableClients.getClientInfo(contact.getUsername());

            if (availableClient != null) {
                Gson gson = new Gson();
                String jsonMessage;
                PrintWriter writer = new PrintWriter(availableClient.getOutputStream(), true);

                Message message = new Message(clientUser.getUsername(), availableClient.getUsername(), command);
                jsonMessage = gson.toJson(message);
                writer.println(jsonMessage + "\n");

                if (!isDisconnectNotification) {
                    writer = new PrintWriter(clientUser.getOutputStream(), true);
                    message.setContent(availableClient.getUsername());
                    message.setReceiver(clientUser.getUsername());

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

    private void Authorize() {
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

                    clientUser = WebConnector.TryAuthorizeUser(message);

                    if (clientUser != null) {

                        clientUser.setIP(clientSocket.getInetAddress().toString());
                        clientUser.setPort(clientSocket.getPort());
                        clientUser.setOutputStream(this.outputStream);

                        // Send the account information back to the client
                        BaseResponse response = new BaseResponse(200, clientUser, "");
                        writer.println(gson.toJson(response));

                        // Push clients to the list of currently available clients
                        // Used to determine whether the client is online when a different client performs a check
                        AvailableClients.storeClient(clientUser);
                        this.NotifyConnectedClients(false);
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
                        this.CloseConnection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (closeConnection) break;
        }

        if (clientUser != null) this.isAuthorized = ClientState.Authorized;
    }

    private void SendThroughPipe(Message message, MessageType msgType) {
        Gson gson = new Gson();
        PrintWriter writer = new PrintWriter(outputStream, true);
        String username;
        String jsonMessage;

        if (msgType == MessageType.Ping) message.FormatPingReply();
        else if (msgType == MessageType.Message) message.FormatMessage();

        jsonMessage = gson.toJson(message);

        // In case that we have received a request from another client, skip sending it back to himself
        if (msgType != MessageType.Request) {
            writer.println(jsonMessage);
        }

        username = message.getReceiver();
        UserInformation receiver = AvailableClients.getClientInfo(username);

        // Send all messages besides ping to the receiver - ping is associated only with the sender
        if (receiver != null && msgType != MessageType.Ping) {
            writer = new PrintWriter(receiver.getOutputStream(), true);
            writer.println(jsonMessage);
        }
    }

    private void GetResponse() {
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
                    message.setSender(clientUser.getUsername());

                    if (message.getCommand().contains("/disconnect")) closeConnection = true;
                    else if (message.getCommand().contains("/ping")) {
                        Message serverMessage = new Message("pong!", this.clientUser.getUsername(), "/ping");
                        SendThroughPipe(serverMessage, MessageType.Ping);
                    }
                    else if (message.getCommand().contains("/request")) {
                        Message requestMessage = new Message(message.getContent(), message.getReceiver(), "/request");
                        SendThroughPipe(requestMessage, MessageType.Request);
                    }
                    else if (message.getCommand().equals("/accept")) {
                        Message acceptRequestMessage = new Message(message.getContent(), message.getReceiver(), "/accept");
                        SendThroughPipe(acceptRequestMessage, MessageType.Request);
                    }
                    else if (message.getCommand().equals("/online")) {
                        Message onlineMsg = new Message(message.getContent(), message.getReceiver(), "/online");
                        SendThroughPipe(onlineMsg, MessageType.Request);
                    }
                    else {
                        SendThroughPipe(message, MessageType.Message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                closeConnection = true;
            } finally {
                if (closeConnection) {
                    System.out.println("Closing connection with socket: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
                    try {
                        this.CloseConnection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (closeConnection) break;
        }
    }

    private void OpenStream() throws IOException {
        this.inputStream = this.clientSocket.getInputStream();
        this.outputStream = this.clientSocket.getOutputStream();
    }

    private void CloseConnection() throws IOException {
        Gson gson = new Gson();
        PrintWriter writer = new PrintWriter(outputStream, true);

        // If there's client, it means that he didn't authorize, therefore no need for a graceful disconnect
        // As well as no need to communicate with the other connected clients
        if (this.clientUser != null) {
            Message message = new Message("OK", this.clientUser.getUsername(), "/disconnect");
            String jsonMessage = gson.toJson(message);
            writer.println(jsonMessage);

            this.NotifyConnectedClients(true);
            AvailableClients.removeClient(this.clientUser);
        }

        if (this.clientSocket != null) this.clientSocket.close();
        if (this.outputStream != null) this.outputStream.close();
        if (this.inputStream != null) this.inputStream.close();

        System.out.println("Connection was successfully closed.");
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(portNumber);
        System.out.println("Server has been successfully started. Listening at port: " + serverSocket.getLocalPort());

        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("Client: " + client.getInetAddress() + ":" + client.getPort() + " connected.");
            ServerSocketListener listener = new ServerSocketListener(client);

            try {
                listener.AskForAuthorization();

                // If no connection was established, just skip to the loop head again and await for connection
                if (!listener.clientSocket.isConnected() || listener.clientUser == null) continue;

                listener.StartServer();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        serverSocket.close();
    }
}