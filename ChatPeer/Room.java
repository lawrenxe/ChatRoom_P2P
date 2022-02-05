package com;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
    private String roomID;
    private ConcurrentHashMap<String, ClientConnections> clientHashMap = new ConcurrentHashMap<>();
    private boolean containsOwnClient = false;

    public Room(String roomID) {
        this.roomID = roomID;
    }

    public String getRoomID() {
        return roomID;
    }

    public boolean isContainsOwnClient() {
        return containsOwnClient;
    }

    public void setContainsOwnClient(boolean bool) {
        this.containsOwnClient = bool;
    }


    public ConcurrentHashMap<String, ClientConnections> getClientHashMap() {
        return clientHashMap;
    }

    public void broadcast(String message) {
        ArrayList<ClientConnections> clientList = new ArrayList<>(clientHashMap.values());
        for (ClientConnections client : clientList) {
            client.sendJson(message);
        }

    }

    public void addClient(ClientConnections client) {
        clientHashMap.put(client.getClientIdentity(), client);
        client.setRoomID(roomID);
    }

    public void removeClient(String identity) {
        clientHashMap.get(identity).setRoomID("");
        clientHashMap.remove(identity);

    }


}