package com;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;


public class ServerConnection implements Runnable {
    private Socket socket;
    private DataInputStream inputStream;
    private PeerClient client;
    private boolean alive = false;

    public ServerConnection(Socket socket, PeerClient client) throws IOException {
        this.socket = socket;
        this.client = client;
        this.inputStream = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        alive = true;

        while (alive) {
            try {
                String input = inputStream.readUTF();
                Packets.ToClient packet = Packets.ToClientDeserialize(input);
                handle(packet);

                System.out.printf("\n[%s] %s> ", client.getRoomid(), client.getIdentity());
            } catch (IOException e) {
                alive = false;
                close();
            }

        }
    }


    private void handle(Packets.ToClient packet) {

        if (packet instanceof Packets.RoomChange) {
            client.roomChange(packet);
        } else if (packet instanceof Packets.ServerMessage) {
            client.message(packet);
        } else if (packet instanceof Packets.Roomlist) {
            client.roomList(packet);
        } else if (packet instanceof Packets.RoomContents) {
            client.roomContents(packet);
        } else if (packet instanceof Packets.Neighbours) {
            client.listNeighbours(packet);
        } else if (packet instanceof Packets.ServerShout) {
            client.shout(packet);
        }
    }

    public void close() {
        try {
            inputStream.close();
            socket.close();
            client.resetState();
            System.out.println("Server has disconnected");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}