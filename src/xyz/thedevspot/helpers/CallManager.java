package xyz.thedevspot.helpers;

import java.util.HashMap;

public final class CallManager {
    private static HashMap<String, UdpListener> callStorage = new HashMap<>();

    private CallManager() { }

    public static void storeCallInfo(String key, UdpListener callListener) {
        callStorage.put(key, callListener);
    }

    public static UdpListener getCallInfo(String key) {
        return callStorage.get(key);
    }

    public static void clearCallInfo(String key) {
        callStorage.remove(key);
    }
}