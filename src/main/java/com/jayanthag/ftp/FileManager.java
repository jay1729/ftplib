package com.jayanthag.ftp;

import com.jayanthag.ftp.models.FileElement;
import com.jayanthag.ftp.models.ServerResponse;

import java.io.IOException;
import java.util.ArrayList;

public interface FileManager {

    public ArrayList<FileElement> ls() throws IOException, Exceptions.ServerResponseException;

    public ServerResponse cwd(String path) throws IOException, Exceptions.ServerResponseException;

    public String pwd() throws IOException, Exceptions.ServerResponseException;

    public boolean makeLocalDir(String dirPath);

    public void changeLocalDir(String newDir);

    public ServerResponse download(String filePath) throws IOException, Exceptions.ServerResponseException;

}
