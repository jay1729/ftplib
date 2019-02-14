package com.jayanthag.ftp;

import com.jayanthag.ftp.models.ServerResponse;

import java.io.IOException;

public interface SendCommand {

    public ServerResponse sendCommand(String command) throws IOException;

    public String readStringInput() throws IOException;
}
