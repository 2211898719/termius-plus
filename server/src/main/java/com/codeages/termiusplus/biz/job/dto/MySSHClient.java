package com.codeages.termiusplus.biz.job.dto;

import lombok.AllArgsConstructor;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@AllArgsConstructor
public class MySSHClient {
    private final SSHClient sshClient ;

    public String executeCommand(String command) throws Exception {
        Session.Command cmd = sshClient.startSession().exec(command);
        InputStream in = cmd.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString().trim();
    }
}
