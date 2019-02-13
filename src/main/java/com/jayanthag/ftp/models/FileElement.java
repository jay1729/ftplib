package com.jayanthag.ftp.models;


import com.jayanthag.ftp.Exceptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class FileElement {

    public String type;
    public int perm;
    public long size;
    public Date lastModified;
    public String name;
    public ArrayList<FileElement> children;
    public FileElement parent;

    public static class PERMISSIONS {
        public static int a = 1;
        public static int c = 1<<1;
        public static int d = 1<<2;
        public static int e = 1<<3;
        public static int f = 1<<4;
        public static int l = 1<<5;
        public static int m = 1<<6;
        public static int p = 1<<7;
        public static int r = 1<<8;
        public static int w = 1<<9;

        protected static char[] all(){
            return new char[]{'a', 'c', 'd', 'e', 'f', 'l', 'm', 'p', 'r', 'w'};
        }
    }

    public static class TypeChoices {
        public static String DIRECTORY = "dir";
        public static String FILE = "file";
    }

    public static String[] TypeChoices(){
        return new String[]{TypeChoices.DIRECTORY, TypeChoices.FILE};
    }

    public static boolean isTypeAllowed(String type){
        if(type.contentEquals(TypeChoices.DIRECTORY)) return true;
        return type.contentEquals(TypeChoices.FILE);
    }

    public static int getPermIntFromChar(char perm){
        char[] permChoices = PERMISSIONS.all();
        for(int i=0;i<permChoices.length;i++){
            if(perm == permChoices[i]) return 1<<i;
        }
        return -1;
    }

    public static String getPermStringFromIntList(int perm){
        char[] permChoices = PERMISSIONS.all();
        StringBuilder permBuilder = new StringBuilder();
        for(int i=0;i<permChoices.length;i++){
            if(((1<<i) & perm) == (1<<i)) permBuilder.append(permChoices[i]);
        }
        return permBuilder.toString();
    }

    public static int getPermIntFromString(String perm){
        int output = 0;
        int size = perm.length();
        for(int i=0;i<size;i++){
            output += getPermIntFromChar(perm.charAt(i));
        }
        return output;
    }

    private static Date getDateFromString(String date) throws ParseException {
        String DATE_FORMAT = "yyyyMMddHHmmss";
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        return formatter.parse(date);
    }

    public FileElement(){}

    public FileElement(String type, int perm, long size,
                       Date lastModified, String name){
        this.type = type;
        this.perm = perm;
        this.size = size;
        this.lastModified = lastModified;
        this.name = name;
    }

    public FileElement(String type, int perm, long size,
                       Date lastModified, String name, ArrayList<FileElement> children){
        this(type, perm, size, lastModified, name);
        this.children = children;
    }

    public FileElement(String type, String perm, String size,
                       String lastModified, String name) throws ParseException{
        this(type, getPermIntFromString(perm), Long.valueOf(size), getDateFromString(lastModified), name);
    }

    public void setPerm(String perm){
        this.perm = getPermIntFromString(perm);
    }

    public void setLastModified(String lastModified) throws ParseException{
        this.lastModified = getDateFromString(lastModified);
    }

    public void setSize(String size){
        this.size = Long.valueOf(size);
    }

    public void setName(String name){
        this.name = name;
    }

    public void setType(String type) throws Exceptions.FileTypeException {
        if(!isTypeAllowed(type)) throw new Exceptions.FileTypeException("Unexpected File Type");
        this.type = type;
    }

}
