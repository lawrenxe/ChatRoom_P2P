/*
Using the starter code shown in COMP90015 tute 5 Packets.
 */
package com;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.HashMap;


public class Packets {

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
    )

    @JsonSubTypes({
        @JsonSubTypes.Type(value = HostChange.class, name = "hostchange"),
        @JsonSubTypes.Type(value = Join.class, name = "join"),
        @JsonSubTypes.Type(value = Who.class, name ="who"),
        @JsonSubTypes.Type(value = List.class, name ="list"),
        @JsonSubTypes.Type(value = CreateRoom.class, name ="createroom"),
        @JsonSubTypes.Type(value = Delete.class, name ="delete"),
        @JsonSubTypes.Type(value = Quit.class, name ="quit"),
        @JsonSubTypes.Type(value = ClientMessage.class, name ="message"),
        @JsonSubTypes.Type(value = ListNeighbours.class, name ="listneighbours"),
        @JsonSubTypes.Type(value = ClientShout.class, name = "shout")
    })

    public static class ToServer {

    }

    @JsonTypeName("hostchange")
    public static class HostChange extends ToServer {
        public String host;
    }

    @JsonTypeName("join")
    public static class Join extends ToServer {
        public String roomid;
    }

    @JsonTypeName("who")
    public static class Who extends ToServer {
        public String roomid;
    }

    @JsonTypeName("list")
    public static class List extends ToServer {

    }

    @JsonTypeName("createroom")
    public static class CreateRoom extends ToServer {
        public String roomid;
    }
    @JsonTypeName("delete")
    public static class Delete extends ToServer {
        public String roomid;
    }

    @JsonTypeName("quit")
    public static class Quit extends ToServer {

    }

    @JsonTypeName("message")
    public static class ClientMessage extends ToServer {
        public String content;
    }

    @JsonTypeName("listneighbours")
    public static class ListNeighbours extends ToServer {

    }

    @JsonTypeName("shout")
    public static class ClientShout extends ToServer {
        public String identity;
        public String message;
        public String[] receivedServers;
    }




    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
    )
    @JsonSubTypes({
        @JsonSubTypes.Type(value = RoomChange.class, name ="roomchange"),
        @JsonSubTypes.Type(value = RoomContents.class, name ="roomcontents"),
        @JsonSubTypes.Type(value = Roomlist.class, name ="roomlist"),
        @JsonSubTypes.Type(value = ServerMessage.class, name ="message"),
        @JsonSubTypes.Type(value = Neighbours.class, name ="neighbours"),
        @JsonSubTypes.Type(value = ServerShout.class, name ="shout")

    })
    public static class ToClient {

    }

    @JsonTypeName("roomchange")
    public static class RoomChange extends ToClient {
        public String identity;
        public String former;
        public String roomid;
    }

    @JsonTypeName("roomcontents")
    public static class RoomContents extends ToClient {
        public String roomid;
        public String[] identities;
    }

    @JsonTypeName("roomlist")
    public static class Roomlist extends ToClient {
        public HashMap<String, Object>[] rooms;
    }

    @JsonTypeName("message")
    public static class ServerMessage extends ToClient {
        public String identity;
        public String content;
    }

    @JsonTypeName("neighbours")
    public static class Neighbours extends ToClient {
        public String[] neighbours;
    }

    @JsonTypeName("shout")
    public static class ServerShout extends ToClient {
        public String identity;
        public String message;
    }

    public static String ToClientSerialize(Packets.ToClient packet) {

        String json = null;
        try {
            json = new ObjectMapper().writeValueAsString(packet);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static String ToServerSerialize(Packets.ToServer packet) {
        String json = null;
        try {
            json = new ObjectMapper().writeValueAsString(packet);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static Packets.ToClient ToClientDeserialize(String json) {
        ToClient packet = null;
        try {
            packet = new ObjectMapper().readerFor(ToClient.class).readValue(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return packet;
    }

    public static Packets.ToServer ToServerDeserialize(String json) {
        ToServer packet = null;
        try {
            packet = new ObjectMapper().readerFor(ToServer.class).readValue(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return packet;
    }

}
