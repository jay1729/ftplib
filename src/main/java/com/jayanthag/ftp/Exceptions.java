package com.jayanthag.ftp;

public class Exceptions {

    public static class AuthFailedException extends Exception{
        public AuthFailedException(){
            super("Incorrect Authentication Credentials!");
        }
    }

    /*
        Generic exception raised when return code is unexpected
     */
    public static class ServerResponseException extends Exception{
        public ServerResponseException(String response){
            super("this reply was not expected from server: "+response);
        }
    }

    public static class FileTypeException extends Exception{
        public FileTypeException(String errorMessage){
            super(errorMessage);
        }
    }

}
