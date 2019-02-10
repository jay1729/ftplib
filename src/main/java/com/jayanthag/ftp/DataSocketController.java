package com.jayanthag.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;

public class DataSocketController {

    private ServerSocket socket;
    private Socket incomingSocket;
    private InputStreamReader inputReader;

    public DataSocketController(InetAddress inetAddress) throws IOException {
        socket = new ServerSocket(0, 0, inetAddress);
    }

    public ServerSocket getSocket(){
        return socket;
    }

    private String readLine() throws IOException{
        StringBuilder builder = new StringBuilder();
        int currentChar = inputReader.read();
        while ((currentChar != -1) && (currentChar != 10)){
            builder.append((char) currentChar);
            currentChar = inputReader.read();
        }
        if(builder.toString().isEmpty()) if(currentChar == -1) return null;
        return builder.toString();
    }

    private ArrayList<String> readLines() throws IOException{
        ArrayList<String> output = new ArrayList<>();
        while (true){
            String currentLine = readLine();
            if(currentLine == null) break;
            output.add(currentLine);
        }
        return output;
    }

    public ArrayList<String> getDataAsStrings() throws IOException{
        incomingSocket = socket.accept();
        inputReader = new InputStreamReader(incomingSocket.getInputStream());
        return readLines();
    }

    public void closeSocket() throws IOException{
        incomingSocket.close();
        socket.close();
    }

}
