package com;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Peer {
    private final int DEFAULT_LISTENING_PORT = 4444;


    @Option(name="-p", usage="port for the server to listen on")
    private int listenPort = DEFAULT_LISTENING_PORT;
    @Option(name="-i", usage="port for socket to connect on")
    private int connectPort;


    public static void main(String[] args) {
        Peer peer = new Peer();
        peer.handle(args);
    }

    private void handle(String[] args) {

        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);

        } catch (CmdLineException e) {

            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }


        PeerServer server = new PeerServer(listenPort, connectPort);

        new Thread(server).start();

    }

}
