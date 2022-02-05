package com;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PeerServer implements Runnable {
    private ConcurrentMap<String, Room> roomHashMap = new ConcurrentHashMap<>();
    private boolean alive = false;
    private ServerSocket serverSocket;
    private int listenPort;
    private ArrayList<String> blackList = new ArrayList<>();
    private ConcurrentMap<String, ClientConnections> connectedClients = new ConcurrentHashMap<>();
    private PeerClient client;
    private String serverIdentity;

    public PeerServer(int listenPort, int connectPort) {
        this.listenPort = listenPort;
        this.alive = true;
        this.client = new PeerClient(connectPort, listenPort, this);
        try {
            serverIdentity = InetAddress.getLocalHost().getHostAddress()+":"+listenPort;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        new Thread(this.client).start();
        try {
            serverSocket = new ServerSocket(listenPort);
        } catch (IOException e) {
            System.out.println("Address already in use");
            System.exit(1);
        }

        // when some other peers connects to me
        while (alive) {
            newClient(serverSocket);
        }
    }

    private void newClient(ServerSocket serverSocket) {
        try {

            Socket socket = serverSocket.accept();
            String ipAddress = socket.getInetAddress().toString();

            if (blackList.contains(ipAddress)){
                socket.close();
            } else {

                ClientConnections clientConnection = new ClientConnections(socket, this);
                String clientIdentity = clientConnection.getClientIdentity();
                connectedClients.put(clientIdentity, clientConnection);

                Thread connection = new Thread(clientConnection);
                connection.start();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void hostIdentity(ClientConnections clientConnection, String identity){
        String clientIdentity = clientConnection.getClientIdentity();
        Packets.RoomChange roomChange = new Packets.RoomChange();
        String json_identity = clientIdentity;
        String json_former = "";
        String json_roomid = "";

        roomChange.identity = json_identity;
        roomChange.former = json_former;
        roomChange.roomid = json_roomid;
        String json_roomChange = Packets.ToClientSerialize(roomChange);
        clientConnection.sendJson(json_roomChange);

        clientConnection.setServerIdentity(identity);
    }

    private boolean isRoomIDValid(String roomID) {
        return (roomID.matches("[a-zA-Z0-9]{3,32}") && roomID.substring(0, 1).matches("[a-zA-Z]"));
    }

    private boolean isRoomIDInUse(String roomID) {
        return (roomHashMap.containsKey(roomID));
    }

    public boolean createRoom(String roomID){
        boolean created;
        if (isRoomIDValid(roomID) && !isRoomIDInUse(roomID)) {
            Room newRoom = new Room(roomID);
            roomHashMap.put(roomID, newRoom);
            created = true;
        } else  {
            created = false;
        }
        return created;
    }

    public boolean deleteRoom(String roomID) {
        boolean deleted;
        if (roomHashMap.containsKey(roomID)) {
            for (ClientConnections clientInRoom : roomHashMap.get(roomID).getClientHashMap().values()) {
                roomChange(clientInRoom, "");
            }
            roomHashMap.remove(roomID);
            deleted = true;
        } else {
            deleted = false;
        }
        return deleted;
    }

    public void kick(String kickedUserIdentity){
        if (connectedClients.containsKey(kickedUserIdentity)) {
            // add to blacklist
            String ipAddress = kickedUserIdentity.split(":")[0];
            blackList.add(ipAddress);
            // disconnect the socket
            connectedClients.get(kickedUserIdentity).close();
        }
    }

    public void roomChange(ClientConnections client, String roomID) {

        String json_identity = client.getClientIdentity();
        String json_former = client.getRoomID();
        String json_roomid;

        boolean isRoomExist = (roomHashMap.containsKey(roomID) || roomID.equals(""));
        if (!isRoomExist) {
            json_roomid = client.getRoomID();

            Packets.RoomChange roomChange = new Packets.RoomChange();
            roomChange.identity = json_identity;
            roomChange.former = json_former;
            roomChange.roomid = json_roomid;

            String json = Packets.ToClientSerialize(roomChange);
            client.sendJson(json);

        } else {

            json_roomid = roomID;
            Packets.RoomChange roomChange = new Packets.RoomChange();
            roomChange.identity = json_identity;
            roomChange.former = json_former;
            roomChange.roomid = json_roomid;

            String json = Packets.ToClientSerialize(roomChange);

            if (!client.getRoomID().equals("")) {
                roomHashMap.get(json_former).broadcast(json);
                roomHashMap.get(client.getRoomID()).removeClient(client.getClientIdentity());

                if (roomHashMap.get(json_former).isContainsOwnClient()) {
                    this.client.roomChange(roomChange);
                }
            }

            if (!json_roomid.equals("")) {
                roomHashMap.get(roomID).addClient(client);
                roomHashMap.get(roomID).broadcast(json);
                if (roomHashMap.get(roomID).isContainsOwnClient()) {
                    this.client.roomChange(roomChange);
                }
            }

        }
    }

    public void roomChange(String identity, String former, String roomID) {
        String json_identity = identity;
        String json_former = former;
        String json_roomid;

        boolean isRoomExist = (roomHashMap.containsKey(roomID) || roomID.equals(""));
        if (!isRoomExist) {
            json_roomid = former;
            Packets.RoomChange roomChange = new Packets.RoomChange();
            roomChange.identity = json_identity;
            roomChange.former = json_former;
            roomChange.roomid = json_roomid;

            this.client.roomChange(roomChange);
        } else {
            json_roomid = roomID;
            Packets.RoomChange roomChange = new Packets.RoomChange();
            roomChange.identity = json_identity;
            roomChange.former = json_former;
            roomChange.roomid = json_roomid;

            String json = Packets.ToClientSerialize(roomChange);

            if (!former.equals("")) {
                roomHashMap.get(json_former).setContainsOwnClient(false);
                roomHashMap.get(json_former).broadcast(json);
            }

            if (!json_roomid.equals("")) {
                roomHashMap.get(roomID).setContainsOwnClient(true);
                roomHashMap.get(roomID).broadcast(json);
            }

            this.client.roomChange(roomChange);
        }
    }


    public void roomList(ClientConnections client) {


        ArrayList<HashMap<String, Object>> json_rooms = new ArrayList<>();

        for (String key : roomHashMap.keySet()) {
            HashMap<String, Object> room = new HashMap<>();
            room.put("roomid", roomHashMap.get(key).getRoomID());
            int count = roomHashMap.get(key).getClientHashMap().size();
            if (roomHashMap.get(key).isContainsOwnClient()) {
                count++;
            }
            room.put("count", count);
            json_rooms.add(room);
        }

        HashMap<String, Object>[] json_roomArray = json_rooms.toArray(new HashMap[json_rooms.size()] );
        Packets.Roomlist roomlist = new Packets.Roomlist();
        roomlist.rooms = json_roomArray;

        String json = Packets.ToClientSerialize(roomlist);
        if (client != null) {
            client.sendJson(json);
        } else {
            this.client.roomList(roomlist);
        }
    }

    public void message(ClientConnections client, String content) {
        String json_content = content;
        String json_identity = client.getClientIdentity();

        Packets.ServerMessage serverMessage = new Packets.ServerMessage();
        serverMessage.identity = json_identity;
        serverMessage.content = json_content;

        String json = Packets.ToClientSerialize(serverMessage);
        roomHashMap.get(client.getRoomID()).broadcast(json);
        if (roomHashMap.get(client.getRoomID()).isContainsOwnClient()) {
            this.client.message(serverMessage);
        }
    }

    public void message(String identity, String roomID, String content) {
        String json_content = content;
        String json_identity = identity;

        Packets.ServerMessage serverMessage = new Packets.ServerMessage();
        serverMessage.identity = json_identity;
        serverMessage.content = json_content;

        String json = Packets.ToClientSerialize(serverMessage);
        roomHashMap.get(roomID).broadcast(json);
        this.client.message(serverMessage);

    }


    public void quit(ClientConnections client) {

        String json_identity = client.getClientIdentity();
        String json_former = client.getRoomID();
        String json_roomid = "";

        Packets.RoomChange roomChange = new Packets.RoomChange();
        roomChange.identity = json_identity;
        roomChange.former = json_former;
        roomChange.roomid = json_roomid;
        String json = Packets.ToClientSerialize(roomChange);

        if (client.isAlive()) {
            client.sendJson(json);
        }

        if (!json_former.equals("")) {
            roomHashMap.get(client.getRoomID()).removeClient(client.getClientIdentity());
            roomHashMap.get(json_former).broadcast(json);
            if (roomHashMap.get(json_former).isContainsOwnClient()) {
                this.client.roomChange(roomChange);
            }
        }

        client.endConnection();
    }

    public void roomContents(ClientConnections client, String roomID) {

        if (isRoomIDInUse(roomID)) {
            String json_roomid = roomID;
            String[] json_identities;

            ArrayList<String> identitiesArrayList = new ArrayList<>(roomHashMap.get(roomID).getClientHashMap().keySet());
            if (roomHashMap.get(roomID).isContainsOwnClient()) {
                identitiesArrayList.add(serverIdentity);
            }
            json_identities = new String[identitiesArrayList.size()];
            json_identities = identitiesArrayList.toArray(json_identities);


            Packets.RoomContents roomContents = new Packets.RoomContents();
            roomContents.roomid = json_roomid;
            roomContents.identities = json_identities;

            String json = Packets.ToClientSerialize(roomContents);
            if (client!=null) {
                client.sendJson(json);
            } else {
                this.client.roomContents(roomContents);
            }
        }
    }

    public void listNeighbors(ClientConnections client){
        Set<String> neighbourList = new HashSet<>();

        for (Map.Entry<String, ClientConnections> entry: connectedClients.entrySet()){
            if (client != null) {
                if (!client.getClientIdentity().equals(entry.getKey())) {
                    System.out.println(entry.getKey());
                    neighbourList.add(entry.getValue().getServerIdentity());
                }
            } else {
                neighbourList.add(entry.getValue().getServerIdentity());
            }
        }

        if (this.client.isConnected()) {
            neighbourList.add(this.client.getConnectedPeerIdentity());
        }

        String[] json_neighbourList = neighbourList.toArray(new String[neighbourList.size()]);

        Packets.Neighbours neighbours = new Packets.Neighbours();
        neighbours.neighbours = json_neighbourList;
        String json = Packets.ToClientSerialize(neighbours);

        if (client != null) {
            client.sendJson(json);
        } else {
            this.client.listNeighbours(neighbours);
        }
    }

    public void toClientShout(String identity, String message) {
        String json_message = message;
        String json_identity = identity;

        Packets.ServerShout serverShout = new Packets.ServerShout();
        serverShout.identity = json_identity;
        serverShout.message = json_message;
        String json = Packets.ToClientSerialize(serverShout);
        for (Room room : roomHashMap.values()) {
            room.broadcast(json);
            if (room.isContainsOwnClient()) {
                this.client.shout(serverShout);
            }
        }

    }

    public void toServerShout(Packets.ToServer packet) {
        client.toServerShout(packet);
    }






}
