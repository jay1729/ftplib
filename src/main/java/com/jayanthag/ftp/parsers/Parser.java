package com.jayanthag.ftp.parsers;

import com.jayanthag.ftp.models.FileElement;

import java.util.ArrayList;

public interface Parser {

    public ArrayList<FileElement> parse(ArrayList<String> commandOutput);

    public void setCWD(String path);

}
