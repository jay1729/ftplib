package com.jayanthag.ftp;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jayanthag.ftp.Constants.CREDENTIALS;
import com.jayanthag.ftp.models.FTPSocket;
import com.jayanthag.ftp.models.FileElement;
import com.jayanthag.ftp.models.ServerResponse;
import com.jayanthag.ftp.parsers.MLSDParser;
import com.jayanthag.ftp.parsers.Parser;

public class FTPClient implements FileManager, SendCommand{

    private String host;
    private int port;
    private Socket controlSocket;
    private InputStreamReader inputReader;
    private PrintWriter outputWriter;
    protected String username;
    protected String password;
    protected String downloadDir;
    protected String currentWorkingDir;
    protected boolean passiveMode;
    protected boolean debugMode;

    public FTPClient(String host, int port){
        this.host = host;
        this.port = port;
        passiveMode = false;
    }

    public FTPClient(String host, int port, boolean passiveMode){
        this(host, port);
        this.passiveMode = passiveMode;
    }

    public void setPassiveMode(boolean passiveMode){
        this.passiveMode = passiveMode;
    }

    public void setDebugMode(boolean debugMode){
        this.debugMode = debugMode;
    }

    public String readStringInput() throws IOException{
        StringBuilder builder = new StringBuilder();
        int currentChar = inputReader.read();
        while ((currentChar != -1) && (currentChar != 10)){
            builder.append((char) currentChar);
            currentChar = inputReader.read();
        }
        String output = builder.toString();
        if(debugMode) System.out.println("Response - "+output);
        return output;
    }

    private void _sendCommand(String command){
        if(debugMode) System.out.println("Sent - "+command);
        outputWriter.print(command+Constants.CRLF);
        outputWriter.flush();
    }

    public ServerResponse sendCommand(String command) throws IOException{
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

    protected void setCWD(String cwdMsg){
        StringBuilder builder = new StringBuilder();
        int size = cwdMsg.length();
        boolean startRecording = false;
        for(int i=0;i<size;i++){
            if(startRecording){
                if(cwdMsg.charAt(i) == '"') break;
                builder.append(cwdMsg.charAt(i));
            }
            if(cwdMsg.charAt(i) == '"') startRecording = true;
        }
        currentWorkingDir = builder.toString();
    }

    protected void loadCWD() throws IOException, Exceptions.ServerResponseException {
        ServerResponse serverResponse = sendFinalCommand("PWD");
        setCWD(serverResponse.responseMessage);
    }

    public String pwd() throws IOException, Exceptions.ServerResponseException {
        ServerResponse response = sendFinalCommand("PWD");
        setCWD(response.responseMessage);
        return response.responseMessage;
    }

    public ServerResponse cwd(String dirName) throws IOException, Exceptions.ServerResponseException{
        if(dirName.contentEquals("//root")){
            ServerResponse serverResponse = new ServerResponse();
            serverResponse.responseCode = 200;
            serverResponse.responseMessage = "";
            return serverResponse;
        }
        return sendFinalCommand("CWD "+dirName);
    }

    protected Parser getParserForMLSD(){
        return new MLSDParser(this);
    }

    // list the contents of current working directory
    public ArrayList<FileElement> ls() throws IOException, Exceptions.ServerResponseException {
        loadCWD();
        DataSocketController dataSocketController = new DataSocketController(this,
                controlSocket.getInetAddress(), passiveMode);
        ArrayList<String> lines = dataSocketController.getDataAsStrings("MLSD");
        Parser parser = getParserForMLSD();
        parser.setCWD(currentWorkingDir);
        return parser.parse(lines);
    }

    public ArrayList<FileElement> search(String regex, String path) throws IOException, Exceptions.ServerResponseException,
            Exceptions.FileTypeException {
        currentWorkingDir = path;
        cwd(currentWorkingDir);
        return search(regex);
    }

    public ArrayList<FileElement> search(String regex) throws IOException, Exceptions.ServerResponseException,
            Exceptions.FileTypeException {
        ArrayList<FileElement> allFiles = ls();
        ArrayList<FileElement> output = new ArrayList<>();
        for(FileElement file : allFiles){
            file.loadTree(regex);
            if((file.type.contentEquals(FileElement.TypeChoices.DIRECTORY)) && ((file.children == null) || (file.children.size() == 0))) continue;
            if(file.type.contentEquals(FileElement.TypeChoices.FILE)){
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(file.name);
                if(!matcher.find()) continue;
            }
            output.add(file);
        }
        return output;
    }

    public void regexDownload(String regex) throws IOException, Exceptions.ServerResponseException,
            Exceptions.FileTypeException, Exceptions.FileException {
        ArrayList<FileElement> files = search(regex);
        for(FileElement file : files){
            file.download(regex);
        }
    }

    public void setDownloadDir(String downloadDir){
        this.downloadDir = downloadDir;
    }

    public ServerResponse download(String filePath, String downloadDir) throws IOException, Exceptions.ServerResponseException {
        setDownloadDir(downloadDir);
        return download(filePath);
    }

    public ServerResponse download(String filePath) throws IOException, Exceptions.ServerResponseException {
        loadCWD();
        DataSocketController dataSocketController = new DataSocketController(this, controlSocket.getInetAddress(),
                passiveMode, downloadDir);
        return dataSocketController.saveBinaryData(filePath);
    }

    protected void goToPrevLocalDir(){
        int index = downloadDir.lastIndexOf('/');
        downloadDir = downloadDir.substring(0, index);
    }

    protected String appendDirToPath(String path, String dir){
        if(path.lastIndexOf('/') == (path.length()-1)){
            return path+dir;
        }else return path+'/'+dir;
    }

    public void changeLocalDir(String newDir){
        if(newDir.contentEquals("..")){
            goToPrevLocalDir();
            return;
        }
        downloadDir = appendDirToPath(downloadDir, newDir);
    }

    public boolean makeLocalDir(String dirName){
        String dirPath = appendDirToPath(downloadDir, dirName);
        boolean fileCreated = new File(dirPath).mkdir();
        if(debugMode) System.out.println("Trying to create new Dir - "+dirPath+" "+fileCreated);
        return fileCreated;
    }

    public void stopClient() throws IOException{
        controlSocket.close();
    }

    private void throwExceptionAndExit(Exception e) throws Exception{
        stopClient();
        throw e;
    }
}