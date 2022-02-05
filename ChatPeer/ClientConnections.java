package com;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientConnections implements Runnable {
    private String roomID = "";
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Socket socket;
    private boolean alive;
    private PeerServer server;
    private String clientIdentity;
    private String serverIdentity;

    public ClientConnections(Socket socket, PeerServer server) throws IOException {
        this.socket = socket;
        this.outputStream = new DataOutputStream(socket.getOutputStream());
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.alive = true;
        this.server = server;
        this.clientIdentity = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }


    public String getServerIdentity(){
        return serverIdentity;
    }

    public String getClientIdentity(){
        return clientIdentity;
    }

    public String getRoomID(){
        return roomID;
    }

    public void setRoomID(String roomID){
        this.roomID = roomID;
    }

    public Boolean isAlive() {
        return alive;
    }

    public void setServerIdentity(String identity) {
        this.serverIdentity = identity;
    }

    @Override
    public void run() {

        alive = true;

        while (alive) {
            try{

                String input = inputStream.readUTF();
                Packets.ToServer packet = Packets.ToServerDeserialize(input);
                handle(packet);

            } catch (IOException e) {
                alive = false;
                server.quit(this);
            }
        }
    }

    private void handle(Object packet) {

        if (packet instanceof Packets.Join) {
            server.roomChange(this, ((Packets.Join) packet).roomid);
        } else if (packet instanceof Packets.Who) {
            server.roomContents(this, ((Packets.Who) packet).roomid);
        } else if (packet instanceof Packets.List) {
            server.roomList(this);
        } else if (packet instanceof Packets.Quit) {
            server.quit(this);
        } else if (packet instanceof Packets.ClientShout){
            server.toServerShout((Packets.ClientShout) packet);
        } else if (packet instanceof Packets.ListNeighbours) {
            server.listNeighbors(this);
        } else if (packet instanceof Packets.HostChange){
            server.hostIdentity(this, ((Packets.HostChange) packet).host);
        }
        else {
            server.message(this, ((Packets.ClientMessage) packet).content);
        }

    }
    public void close() {
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void endConnection(){
        alive = false;
        close();
    }

    public void sendJson(String json){
        try {
            outputStream.writeUTF(json);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
