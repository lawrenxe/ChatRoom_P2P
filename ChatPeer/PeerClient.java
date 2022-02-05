package com;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class PeerClient implements Runnable {
    private volatile String identity = "";
    private volatile String roomid = "";
    private int defaultOutPort;
    private int defaultInPort;
    private String serverIdentity;


    private PeerServer server;
    private boolean isConnected = false;

    private boolean alive = false;
    private Socket socket;
    private ServerConnection serverConnection;

    private Scanner scanner;
    private DataOutputStream outputStream;

    private boolean sentQuit = false;
    private String connectedPeerIdentity;
//    private boolean searchingNetwork = false;

    private static final String JOIN = "join";
    private static final String WHO = "who";
    private static final String LIST = "list";
    private static final String CREATE_ROOM = "createroom";
    private static final String DELETE_ROOM = "delete";
    private static final String QUIT = "quit";
    private static final String KICK = "kick";
    private static final String CONNECT = "connect";
    private static final String HELP = "help";
    private static final String LIST_NEIGHBORS = "listneighbours";
    private static final String SEARCH_NETWORK = "searchnetwork";
    private static final String SHOUT = "shout";

    public PeerClient(int defaultOutPort, int defaultInPort, PeerServer server) {
        this.server = server;
        this.defaultOutPort = defaultOutPort;
        this.defaultInPort = defaultInPort;
    }

    @Override
    public void run() {
        try {

            setServerIdentity();
            identity = serverIdentity;
            connectedPeerIdentity = serverIdentity;
            clientInput();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setServerIdentity() throws UnknownHostException {
        serverIdentity = InetAddress.getLocalHost().getHostAddress()+":"+defaultInPort;
    }

    private  void clientInput() throws IOException {
        System.out.printf("[%s] %s> ", this.roomid, this.identity);
        scanner = new Scanner(System.in);
        alive = true;
        while (alive) {
            try {
                if (scanner.hasNext()) {
                    String input = scanner.nextLine();
                    if (!input.isEmpty()) {
                        String json = null;
                        if (input.charAt(0) == ('#')) {
                            String[] cmd = input.substring(1).split(" ");
                            boolean missingArg = missingArgument(cmd);
                            switch (cmd[0].toLowerCase()) {
                                case CREATE_ROOM:
                                    if (!isConnected) {
                                        if (!missingArg) {
                                            String roomID = cmd[1];
                                            boolean created = server.createRoom(roomID);
                                            if (created) {
                                                System.out.printf("Room %s created.", roomID);
                                            } else {
                                                System.out.printf("Room %s is invalid or already in use.", roomID);
                                            }

                                        }
                                    }
                                    break;

                                case DELETE_ROOM:
                                    if (!isConnected) {
                                        if (!missingArg) {
                                            String roomID = cmd[1];
                                            boolean deleted = server.deleteRoom(roomID);
                                            if (deleted) {
                                                System.out.printf("Room %s deleted.", roomID);
                                            } else {
                                                System.out.printf("Room %s is invalid.", roomID);
                                            }
                                        }
                                    }
                                    break;

                                case KICK:
                                    if (!isConnected) {
                                        if (!missingArg) {
                                            String kickedUserIdentity = cmd[1];
                                            server.kick(kickedUserIdentity);
                                        }
                                    }
                                    break;

                                case HELP:
                                    if (!isConnected) {
                                        System.out.println("#list - responds with a list of rooms that the peer is maintaining");
                                        System.out.println("#join roomid - allow a peer to join a room");
                                        System.out.println("#who roomid - provides a list of who is in the room");
                                        System.out.println("#kick IP:Port - the kicked user is forcibly disconnected from the peer and not allowed to join");
                                        System.out.println("#quit - disconnect from the peer");
                                        System.out.println("#connect IP[:port] [local port] - connect to another peer");
                                        System.out.println("#createroom roomid - create a room (local only)");
                                        System.out.println("#delete roomid - delete a room (local only)");
                                        System.out.println("#listneighbour - provides a list of neighbours connect to the peer");
                                        System.out.println("#searchnetwork - provides a list of all rooms on all accessible peers");
                                        System.out.println("#shout message - sends a message to all rooms on all peers in the network");
                                        System.out.println("Message - the message is sent to all peers in the room");

                                    }
                                    break;

                                case CONNECT:
                                    if (!isConnected) {
                                        identity = "";
                                        connectedPeerIdentity = cmd[1];
                                        String ipAddress = cmd[1].split(":")[0];
                                        int port = Integer.parseInt(cmd[1].split(":")[1]);

                                        InetAddress ownINetAddress = InetAddress.getLocalHost();
                                        int outGoingPort = cmd.length == 3 ? Integer.parseInt(cmd[2]) : defaultOutPort;
                                        if (outGoingPort == 0) {
                                            this.socket = new Socket(ipAddress, port);
                                        } else {
                                            this.socket = new Socket(ipAddress, port, ownINetAddress, outGoingPort);
                                        }
                                        this.outputStream = new DataOutputStream(socket.getOutputStream());
                                        Packets.HostChange hostChange = new Packets.HostChange();
                                        hostChange.host = String.format("%s:%s", InetAddress.getLocalHost().getHostAddress(), defaultInPort);
                                        json = Packets.ToServerSerialize(hostChange);

                                        serverConnection = new ServerConnection(socket, this);
                                        new Thread(serverConnection).start();
                                        this.isConnected = true;
                                    }
                                    break;

                                case JOIN:
                                    String newRoom = missingArg ? "" : cmd[1];
                                    if (isConnected) {
                                        Packets.Join join = new Packets.Join();
                                        join.roomid = newRoom;
                                        json = Packets.ToServerSerialize(join);
                                    } else {
                                        server.roomChange(identity, roomid, newRoom);
                                    }
                                    missingArg = false;
                                    break;

                                case WHO:
                                    if (!missingArg) {
                                        if (isConnected) {
                                            Packets.Who who = new Packets.Who();
                                            who.roomid = cmd[1];
                                            json = Packets.ToServerSerialize(who);
                                        } else {
                                            server.roomContents(null, roomid);
                                        }
                                    }
                                    break;
                                case LIST:
                                    missingArg = false;
                                    if (isConnected) {
                                        Packets.List list = new Packets.List();
                                        json = Packets.ToServerSerialize(list);
                                    } else {
                                        server.roomList(null);
                                    }
                                    break;

                                case QUIT:
                                    missingArg = false;
                                    if (isConnected) {
                                        Packets.Quit quit = new Packets.Quit();
                                        json = Packets.ToServerSerialize(quit);
                                        sentQuit = true;
                                    }
                                    break;

                                case LIST_NEIGHBORS:
                                    missingArg = false;
                                    if (isConnected) {
                                        Packets.ListNeighbours listNeighbours = new Packets.ListNeighbours();
                                        json = Packets.ToServerSerialize(listNeighbours);

                                    } else {
                                        server.listNeighbors(null);
                                    }
                                    break;

//                                case SEARCH_NETWORK:
//                                    missingArg = false;
//                                    searchingNetwork = true;
//                                    this.searchNetwork();

                                case SHOUT:
                                    if (!missingArg) {
                                        if (!roomid.equals("")) {
                                            if (isConnected) {
                                                String[] receivedServers = {serverIdentity};
                                                Packets.ClientShout shout = new Packets.ClientShout();
                                                shout.identity = this.identity;
                                                shout.message = input.split(" ", 2)[1];
                                                shout.receivedServers = receivedServers;
                                                json = Packets.ToServerSerialize(shout);

                                            } else {
                                                server.toClientShout(identity, input.split(" ", 2)[1]);
                                            }
                                        }
                                    }
                                    break;

                                default:
                                    missingArg = false;
                                    System.out.println("Invalid command");

                            }
                            if (missingArg) {
                                System.out.printf("Missing argument");
                            }
                        } else {
                            if (!roomid.equals("")) {
                                if (isConnected) {
                                    Packets.ClientMessage message = new Packets.ClientMessage();
                                    message.content = input;
                                    json = Packets.ToServerSerialize(message);
                                } else {
                                    server.message(identity, roomid, input);
                                }
                            }

                        }
                        if (json != null) {
                            outputStream.writeUTF(json);
                            outputStream.flush();
                        } else {
                            System.out.printf("\n[%s] %s> ", this.roomid, this.identity);
                        }
                    }
                } else {
                    close();
                }
            } catch (IOException e) {

            }
        }
    }



    public void close() {
        try {
            if (isConnected) {
                outputStream.close();
                serverConnection.close();
                socket.close();
            }
            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean missingArgument(String[] cmd) {
        if (cmd.length == 1) {
            return true;
        }
        return false;
    }

    public String getIdentity() {
        return identity;
    }

    public void resetState() {
        connectedPeerIdentity = serverIdentity;
        identity = serverIdentity;
        roomid = "";
        isConnected = false;
        sentQuit = false;
    }

    public String getConnectedPeerIdentity() {
        return connectedPeerIdentity;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getRoomid() {
        return roomid;
    }

    private void disconnect() {
        System.out.printf("\nDisconnected from %s", connectedPeerIdentity);
        resetState();
        try {
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void roomChange(Packets.ToClient packet) {
        String roomid = ((Packets.RoomChange) packet).roomid;
        String former = ((Packets.RoomChange) packet).former;
        String identity = ((Packets.RoomChange) packet).identity;

        if (roomid.equals("") && former.equals("")) {
            if (sentQuit) {
                disconnect();
            }
            else if (this.identity.equals("")) {
                this.identity = identity;
            } else {
                System.out.print("\nThe requested room is invalid or non existent.");
            }
        }

        else if (roomid.equals("")) {
            System.out.printf("\n%s leaves %s", identity, former);

            if (this.identity.equals(identity)) {
                if (sentQuit) {
                    disconnect();
                }

                this.roomid = roomid;
            }

        } else if (!former.equals(roomid)) {
            if (former.equals("")) {
                System.out.printf("\n%s moves to %s", identity, roomid);
            } else {
                System.out.printf("\n%s moves from %s to %s", identity, former, roomid);
            }

            if (this.identity.equals(identity)) {
                this.roomid = roomid;
            }

        } else {
            System.out.print("\nThe requested room is invalid or non existent.");
        }
    }

    public void roomList(Packets.ToClient packet) {
        HashMap<String, Object>[] rooms = ((Packets.Roomlist) packet).rooms;


        for (HashMap<String, Object> room : rooms) {
            System.out.printf("\n%s: %d guests", room.get("roomid"), room.get("count"));
        }


    }

    public void message(Packets.ToClient packet) {
        String identity = ((Packets.ServerMessage) packet).identity;
        String content = ((Packets.ServerMessage) packet).content;
        if (!identity.equals(this.identity)) {
            System.out.println();
        }
        System.out.printf("%s: %s", identity, content);
        if (!isConnected) {
            System.out.printf("\n[%s] %s> ", this.roomid, this.identity);
        }

    }

    public void shout(Packets.ToClient packet) {
        String identity = ((Packets.ServerShout) packet).identity;
        String message = ((Packets.ServerShout) packet).message;


        System.out.printf("\n%s shouted %s", identity, message);
        if (isConnected) {
            server.toClientShout(identity, message);
        }
    }

    public void toServerShout(Packets.ToServer packet) {
        String json_message = ((Packets.ClientShout) packet).message;
        String json_identity = ((Packets.ClientShout) packet).identity;
        String[] receivedServers = ((Packets.ClientShout) packet).receivedServers;

        boolean received = false;
        for (String identity : receivedServers) {
            if (isConnected && connectedPeerIdentity.equals(identity)) {
                received = true;
                break;
            }
        }

        if (isConnected && !received ) {
            ArrayList<String> temp = new ArrayList<>(Arrays.asList(receivedServers));
            temp.add(this.getConnectedPeerIdentity());
            String[] json_receivedServers = new String[temp.size()];
            temp.toArray(json_receivedServers);
            Packets.ClientShout clientShout = new Packets.ClientShout();
            clientShout.identity = json_identity;
            clientShout.message = json_message;
            clientShout.receivedServers = json_receivedServers;
            String json = Packets.ToServerSerialize(clientShout);

            try {
                outputStream.writeUTF(json);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            server.toClientShout(json_identity, json_message);
        }


    }

    public void roomContents(Packets.ToClient packet) {
        String roomid = ((Packets.RoomContents) packet).roomid;
        String[] identities = ((Packets.RoomContents) packet).identities;
        String owner = connectedPeerIdentity;

        System.out.printf("\n%s contains ", roomid);
        for (String client : identities) {
            if (client.equals(owner)) {
                client = client + "*";
            }
            System.out.printf("%s ", client);
        }
    }

    public void listNeighbours(Packets.ToClient packet) {
        String[] neighbours = ((Packets.Neighbours) packet).neighbours;
        for (String neighbour : neighbours) {
            System.out.printf("\n%s", neighbour);
        }

    }

//    private void searchNetwork() throws IOException{
//        ArrayList<String> visited = new ArrayList<>();
//        ArrayList<String> queue = new ArrayList<>();
//        if (isConnected) {
//            Packets.ListNeighbours listNeighbours = new Packets.ListNeighbours();
//            String json = Packets.ToServerSerialize(listNeighbours);
//            outputStream.writeUTF(json);
//            outputStream.flush();
//        } else {
//            server.listNeighbors(null);
//        }
//
//        // array [b]
//        // queue [b]
//        // b -> [c,d,e]
//        // queue [c, d, e]
//        // c -> [b]
//        // queue [d.e]
//
//    }

}
