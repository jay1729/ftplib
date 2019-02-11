package com.jayanthag.ftp.models;

public class ServerResponse {

    public int responseCode;
    public String responseMessage;

    public static ServerResponse getResponseFromString(String response){
        ServerResponse serverResponse = new ServerResponse();
        serverResponse.responseCode = Integer.valueOf(response.substring(0, 3));
        serverResponse.responseMessage = response.substring(4, response.length());
        return serverResponse;
    }
}
