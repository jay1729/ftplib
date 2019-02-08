package com.jayanthag.ftp;

public class Exceptions {

    public static class AuthFailedException extends Exception{
        public AuthFailedException(){
            super("Incorrect Authentication Credentials!");
        }
    }

}
