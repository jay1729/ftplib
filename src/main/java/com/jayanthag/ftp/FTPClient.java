package com.jayanthag.ftp;

import java.io.*;
import java.net.*;
import java.time.temporal.ChronoUnit;

import com.jayanthag.ftp.Constants.CREDENTIALS;

public class FTPClient {

    private String host;
    private int port;
    private Socket socket;
    private InputStreamReader inputReader;
    private PrintWriter outputWriter;
    private String username;
    private String password;

    public FTPClient(String host, int port){
        this.host = host;
        this.port = port;
    }

    private String readStringInput() throws IOException{
        StringBuilder builder = new StringBuilder();
        int currentChar = inputReader.read();
        while ((currentChar != -1) && (currentChar != 10)){
            builder.append((char) currentChar);
            currentChar = inputReader.read();
        }
        return builder.toString();
    }

    private void _sendCommand(String command){
        outputWriter.print(command+Constants.CRLF);
        outputWriter.flush();
    }

    private ServerResponse sendCommand(String command) throws IOException{
        _sendCommand(command);
        String responseString = readStringInput();
        return ServerResponse.getResponseFromString(responseString);
    }

    public ServerResponse connect() throws IOException{
        socket = new Socket(host, port);
        inputReader = new InputStreamReader(socket.getInputStream());
        outputWriter = new PrintWriter(socket.getOutputStream(), true);
        return ServerResponse.getResponseFromString(readStringInput());
    }

    public ServerResponse login() throws Exception{
        return login(CREDENTIALS.ANON_USERNAME, CREDENTIALS.ANON_PASS);
    }

    public ServerResponse login(String username, String password) throws Exception{
        this.username = username;
        this.password = password;
        sendCommand("USER " + username);
        ServerResponse serverResponse = sendCommand("PASS " + password);
        if((serverResponse.responseCode/100) != 2) throwExceptionAndExit(new Exceptions.AuthFailedException());
        return serverResponse;
    }

    public void stopClient() throws IOException{
        socket.close();
    }

    private void throwExceptionAndExit(Exception e) throws Exception{
        stopClient();
        throw e;
    }
}