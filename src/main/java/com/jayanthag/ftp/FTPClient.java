package com.jayanthag.ftp;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import com.jayanthag.ftp.Constants.CREDENTIALS;

public class FTPClient {

    private String host;
    private int port;
    private Socket controlSocket;
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
        controlSocket = new Socket(host, port);
        System.out.println("YYY"+controlSocket.getLocalSocketAddress()+" "+controlSocket.getPort());
        inputReader = new InputStreamReader(controlSocket.getInputStream());
        outputWriter = new PrintWriter(controlSocket.getOutputStream(), true);
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

    private ServerResponse sendPort(String address) throws IOException{
        String[] s = address.split(":", 2);
        String host = s[0];
        host = host.replace('.', ',');
        int port = Integer.valueOf(s[1]);
        String addressInfo = host.substring(1) + ',' + String.valueOf(port/256) + ',' + String.valueOf(port%256);
        return sendCommand("PORT "+addressInfo);
    }

    private void sendPort(ServerSocket serverSocket) throws IOException{
        ServerResponse response = sendPort(serverSocket.getLocalSocketAddress().toString());
        System.out.println(response.responseCode+" "+response.responseMessage);
    }

    // list the contents of current working directory
    public ArrayList<String> ls() throws IOException{
        System.out.println("127.0.0.1".split(".", 2)[0]);
        ServerResponse response = sendCommand("TYPE A");
        System.out.println(response.responseCode+" "+response.responseMessage);
        response = sendCommand("PWD");
        System.out.println(response.responseCode+" "+response.responseMessage);
        response = sendCommand("LIST");
        System.out.println(response.responseCode+" "+response.responseMessage);
        DataSocketController dataSocketController = new DataSocketController(controlSocket.getInetAddress());
        sendPort(dataSocketController.getSocket());
        return null;
    }

    public void stopClient() throws IOException{
        controlSocket.close();
    }

    private void throwExceptionAndExit(Exception e) throws Exception{
        stopClient();
        throw e;
    }
}