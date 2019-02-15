package com.jayanthag.ftp;

import com.jayanthag.ftp.models.FTPSocket;
import com.jayanthag.ftp.models.ServerResponse;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataSocketController {

    private ServerSocket socket;
    private Socket dataSocket;
    private InputStreamReader inputReader;
    private BufferedInputStream binaryInputStream;
    private BufferedOutputStream binaryOutputStream;
    protected String downloadDir;
    protected boolean passiveMode;
    protected SendCommand ftpClient;
    protected InetAddress mainChannelAddress;

    public DataSocketController(SendCommand ftpClient, InetAddress inetAddress, boolean passiveMode){
        mainChannelAddress = inetAddress;
        this.ftpClient = ftpClient;
        this.passiveMode = passiveMode;
    }

    public DataSocketController(SendCommand ftpClient, InetAddress inetAddress, boolean passiveMode, String downloadDir){
        this(ftpClient, inetAddress, passiveMode);
        this.downloadDir = downloadDir;
    }

    protected ServerResponse sendVoidCommand(String command, int expectedCode) throws IOException, Exceptions.ServerResponseException {
        ServerResponse response = ftpClient.sendCommand(command);
        if((response.responseCode/100) != expectedCode) throw new Exceptions.ServerResponseException(response.responseMessage);
        return response;
    }

    protected ServerResponse sendPreliminaryCommand(String command) throws IOException, Exceptions.ServerResponseException {
        return sendVoidCommand(command, 1);
    }

    protected ServerResponse sendFinalCommand(String command) throws IOException, Exceptions.ServerResponseException {
        return sendVoidCommand(command, 2);
    }

    protected ServerResponse expectFinalReply() throws IOException, Exceptions.ServerResponseException{
        ServerResponse serverResponse = ServerResponse.getResponseFromString(ftpClient.readStringInput());
        if((serverResponse.responseCode/100) != 2) throw new Exceptions.ServerResponseException(serverResponse.responseMessage);
        return serverResponse;
    }

    protected void sendPort(String address) throws IOException, Exceptions.ServerResponseException {
        String[] s = address.split(":", 2);
        String host = s[0];
        host = host.replace('.', ',');
        int port = Integer.valueOf(s[1]);
        String addressInfo = host.substring(1) + ',' + String.valueOf(port/256) + ',' + String.valueOf(port%256);
        sendFinalCommand("PORT "+addressInfo);
    }

    protected void sendPort(ServerSocket serverSocket) throws IOException, Exceptions.ServerResponseException {
        sendPort(serverSocket.getLocalSocketAddress().toString());
    }

    protected FTPSocket parsePASV(String message) throws Exceptions.ServerResponseException {
        String regex = "\\((\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);
        String host = "";
        int port = 0;
        try {
            if(!matcher.find()) throw new Exceptions.ServerResponseException("Unable to parse response to PASV command");
            for(int i=0;i<4;i++){
                host += '.' + matcher.group(i+1);
            }
            host = host.substring(1);
            port += Integer.valueOf(matcher.group(5)) * 256;
            port += Integer.valueOf(matcher.group(6));
        }catch (IllegalStateException e){
            e.printStackTrace();
            throw new Exceptions.ServerResponseException("Unable to parse response to PASV command");
        }
        return new FTPSocket(host, port);
    }

    protected FTPSocket makePassive() throws IOException, Exceptions.ServerResponseException {
        ServerResponse response = sendFinalCommand("PASV");
        return parsePASV(response.responseMessage);
    }

    protected Socket getPassiveSocket() throws IOException, Exceptions.ServerResponseException {
        FTPSocket dataSocketDetails = makePassive();
        return new Socket(dataSocketDetails.host, dataSocketDetails.port);
    }

    protected void setUpPassiveConn() throws IOException, Exceptions.ServerResponseException {
        dataSocket = getPassiveSocket();
    }

    protected void setUpActiveConn() throws IOException, Exceptions.ServerResponseException {
        if(!passiveMode) socket = new ServerSocket(0, 0, mainChannelAddress);
        sendPort(socket);
    }

    protected void acceptActiveConn() throws IOException{
        dataSocket = socket.accept();
    }

    public ServerSocket getSocket(){
        return socket;
    }

    private String readLine() throws IOException{
        StringBuilder builder = new StringBuilder();
        int currentChar = inputReader.read();
        while ((currentChar != -1) && (currentChar != 10)){
            if(currentChar == 13){
                inputReader.read();
                break;
            }
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

    private void readAndStoreBinary() throws IOException{
        int currentByte;
        while ((currentByte = binaryInputStream.read()) != -1){
            binaryOutputStream.write(currentByte);
        }
    }

    public ArrayList<String> getDataAsStrings(String command) throws IOException, Exceptions.ServerResponseException {
        ArrayList<String> output;
        try {
            sendFinalCommand("TYPE A");
            if(!passiveMode) setUpActiveConn();
            else setUpPassiveConn();
            sendPreliminaryCommand(command);
            if(!passiveMode) acceptActiveConn();
            inputReader = new InputStreamReader(dataSocket.getInputStream());
            output = readLines();
        }finally {
            expectFinalReply();
            closeSocket();
        }
        return output;
    }

    protected String parseFileName(String fullPath){
        String[] path = fullPath.split("/");
        return path[path.length-1];
    }

    public ServerResponse saveBinaryData(String filePath) throws IOException, Exceptions.ServerResponseException {
        ServerResponse response;
        try {
            sendFinalCommand("TYPE I");
            if(!passiveMode) setUpActiveConn();
            else setUpPassiveConn();
            sendPreliminaryCommand("RETR "+filePath);
            if(!passiveMode) acceptActiveConn();
            String fileName = parseFileName(filePath);
            binaryOutputStream = new BufferedOutputStream(new FileOutputStream(downloadDir+fileName));
            binaryInputStream = new BufferedInputStream(dataSocket.getInputStream());
            readAndStoreBinary();
        }finally {
            response = expectFinalReply();
            closeSocket();
        }
        return response;
    }

    public void closeSocket() throws IOException{
        if(inputReader != null) inputReader.close();
        if(binaryInputStream != null) binaryInputStream.close();
        if(binaryOutputStream != null) binaryOutputStream.close();
        if(dataSocket != null) dataSocket.close();
        if(socket != null) socket.close();
    }

}
