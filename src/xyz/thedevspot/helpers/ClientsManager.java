package xyz.thedevspot.helpers;

import xyz.thedevspot.models.UserInformation;
import java.util.HashMap;

public class ClientsManager {
    private static HashMap<String, UserInformation> clientMap = new HashMap<>();

    private ClientsManager() { }

    public static HashMap<String, UserInformation> getAvailableClients() {
        return clientMap;
    }

    public static UserInformation getClientInfo(String username) { return clientMap.get(username); }

    public static void storeClient (UserInformation client) {
        clientMap.put(client.getUsername(), client);
    }

    public static void removeClient (UserInformation client) {
        clientMap.remove(client.getUsername());
    }
}
