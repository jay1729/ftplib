package com.jayanthag.ftp;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class DataSocketController {

    private ServerSocket socket;
    private Socket incomingSocket;
    private InputStreamReader inputReader;
    private BufferedInputStream binaryInputStream;
    private BufferedOutputStream binaryOutputStream;

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

    private void readAndStoreBinary() throws IOException{
        int currentByte;
        while ((currentByte = binaryInputStream.read()) != -1){
            binaryOutputStream.write(currentByte);
        }
    }

    public ArrayList<String> getDataAsStrings() throws IOException{
        incomingSocket = socket.accept();
        inputReader = new InputStreamReader(incomingSocket.getInputStream());
        return readLines();
    }

    public void saveBinaryData(String fileName) throws IOException{
        try {
            incomingSocket = socket.accept();
            binaryOutputStream = new BufferedOutputStream(new FileOutputStream(fileName));
            binaryInputStream = new BufferedInputStream(incomingSocket.getInputStream());
            readAndStoreBinary();
        }finally {
            closeSocket();
        }
    }

    public void closeSocket() throws IOException{
        if(inputReader != null) inputReader.close();
        if(binaryInputStream != null) binaryInputStream.close();
        if(binaryOutputStream != null) binaryOutputStream.close();
        if(incomingSocket != null) incomingSocket.close();
        if(socket != null) socket.close();
    }

}
