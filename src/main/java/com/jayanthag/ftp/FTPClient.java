package com.jayanthag.ftp;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import com.jayanthag.ftp.Constants.CREDENTIALS;
import com.jayanthag.ftp.models.FileElement;
import com.jayanthag.ftp.models.ServerResponse;
import com.jayanthag.ftp.parsers.MLSDParser;
import com.jayanthag.ftp.parsers.Parser;

public class FTPClient {

    private String host;
    private int port;
    private Socket controlSocket;
    private InputStreamReader inputReader;
    private PrintWriter outputWriter;
    private String username;
    private String password;
    protected String downloadDir;

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

    private ServerResponse sendVoidCommand(String command, int expectedReplyCode) throws IOException, Exceptions.ServerResponseException {
        ServerResponse serverResponse = sendCommand(command);
        if((serverResponse.responseCode/100) != expectedReplyCode) throw new Exceptions.ServerResponseException(serverResponse.responseMessage);
        return serverResponse;
    }

    private ServerResponse sendFinalCommand(String command) throws IOException, Exceptions.ServerResponseException {
        return sendVoidCommand(command, 2);
    }

    private ServerResponse sendPreliminaryCommand(String command) throws IOException, Exceptions.ServerResponseException {
        return sendVoidCommand(command, 1);
    }

    private ServerResponse sendIntermediateCommand(String command) throws IOException, Exceptions.ServerResponseException {
        return sendVoidCommand(command, 3);
    }

    private ServerResponse expectFinalReply() throws IOException, Exceptions.ServerResponseException{
        ServerResponse serverResponse = ServerResponse.getResponseFromString(readStringInput());
        if((serverResponse.responseCode/100) != 2) throw new Exceptions.ServerResponseException(serverResponse.responseMessage);
        return serverResponse;
    }

    public ServerResponse connect() throws IOException{
        controlSocket = new Socket(host, port);
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

    private void sendPort(String address) throws IOException, Exceptions.ServerResponseException {
        String[] s = address.split(":", 2);
        String host = s[0];
        host = host.replace('.', ',');
        int port = Integer.valueOf(s[1]);
        String addressInfo = host.substring(1) + ',' + String.valueOf(port/256) + ',' + String.valueOf(port%256);
        sendFinalCommand("PORT "+addressInfo);
    }

    private void sendPort(ServerSocket serverSocket) throws IOException, Exceptions.ServerResponseException {
        sendPort(serverSocket.getLocalSocketAddress().toString());
    }

    public String pwd() throws IOException, Exceptions.ServerResponseException {
        return sendFinalCommand("PWD").responseMessage;
    }

    public ServerResponse cwd(String dirName) throws IOException, Exceptions.ServerResponseException{
        return sendFinalCommand("CWD "+dirName);
    }

    protected Parser getParserForMLSD(){
        return new MLSDParser();
    }

    // list the contents of current working directory
    public ArrayList<FileElement> ls() throws IOException, Exceptions.ServerResponseException {
        sendFinalCommand("TYPE A");
        DataSocketController dataSocketController = new DataSocketController(controlSocket.getInetAddress());
        sendPort(dataSocketController.getSocket());
        sendPreliminaryCommand("MLSD");
        ArrayList<String> lines = dataSocketController.getDataAsStrings();
        dataSocketController.closeSocket();
        expectFinalReply();
        Parser parser = getParserForMLSD();
        ArrayList<FileElement> output = parser.parse(lines);
        return output;
    }

    protected String parseFileName(String fullPath){
        String[] path = fullPath.split("/");
        return path[path.length-1];
    }

    public void setDownloadDir(String downloadDir){
        this.downloadDir = downloadDir;
    }

    public ServerResponse download(String filePath, String downloadDir) throws IOException, Exceptions.ServerResponseException {
        setDownloadDir(downloadDir);
        return download(filePath);
    }

    public ServerResponse download(String filePath) throws IOException, Exceptions.ServerResponseException {
        sendFinalCommand("TYPE I");
        DataSocketController dataSocketController = new DataSocketController(controlSocket.getInetAddress(), downloadDir);
        sendPort(dataSocketController.getSocket());
        sendPreliminaryCommand("RETR "+filePath);
        dataSocketController.saveBinaryData(parseFileName(filePath));
        return expectFinalReply();
    }

    public void stopClient() throws IOException{
        controlSocket.close();
    }

    private void throwExceptionAndExit(Exception e) throws Exception{
        stopClient();
        throw e;
    }
}