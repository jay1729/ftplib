package com.jayanthag.ftp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;

public class DataSocketController {

    private ServerSocket socket;
    private Socket incomingSocket;

    public DataSocketController(InetAddress inetAddress) throws IOException {
        socket = new ServerSocket(0, 0, inetAddress);
        System.out.println(socket.getLocalSocketAddress().toString());
    }

    public ServerSocket getSocket(){
        return socket;
    }

}
