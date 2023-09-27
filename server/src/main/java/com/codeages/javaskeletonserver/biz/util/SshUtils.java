package com.codeages.javaskeletonserver.biz.util;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SshUtils {

    public static SshResponse runCommand(SshConnection conn, String cmd, long timeout) throws IOException {
        SshClient client = SshClient.setUpDefaultClient();

        try {
            client.start();
            ConnectFuture cf = client.connect(conn.getUsername(), conn.getHostname(), 22);
            ClientSession session = cf.verify().getSession();
            session.addPasswordIdentity(conn.getPassword());
            session.auth().verify(TimeUnit.SECONDS.toMillis(timeout));

            ChannelExec ce = session.createExecChannel(cmd);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            ce.setOut(out);
            ce.setErr(err);
            ce.open();
            Set<ClientChannelEvent> events = ce.waitFor(
                    EnumSet.of(ClientChannelEvent.CLOSED),
                    TimeUnit.SECONDS.toMillis(timeout)
            );
            session.close(false);

            if (events.contains(ClientChannelEvent.TIMEOUT)) {
                throw new RuntimeException(conn.getHostname() + " 命令 " + cmd + "执行超时 " + timeout);
            }
            return new SshResponse(out.toString(), err.toString(), ce.getExitStatus());

        } finally {
            client.stop();
        }
    }

    public static void main(String[] args) throws IOException {
        String hostName = "121.36.246.56";
        String userName = "root";
        String pwd = "9Ms!2BHnBfmEcNQ62VIr";
        SshConnection conn = new SshConnection(userName, pwd, hostName);
        String cmd = "ll ";
        SshResponse response = runCommand(conn, cmd, 15);
        System.out.println("==error=>" + response.getErrOutput());
        System.out.println("===return==>" + response.getReturnCode());
        System.out.println("===stdOut===>" + response.getStdOutput());
    }

   static class SshResponse{
        private String stdOutput;
        private String errOutput;
        private int returnCode;

        public SshResponse(String stdOutput, String errOutput, int returnCode) {
            this.stdOutput = stdOutput;
            this.errOutput = errOutput;
            this.returnCode = returnCode;
        }

        public String getStdOutput() {
            return stdOutput;
        }

        public String getErrOutput() {
            return errOutput;
        }

        public int getReturnCode() {
            return returnCode;
        }
    }

    static class SshConnection{
        private String username;
        private String password;
        private String hostname;

        public SshConnection(String username, String password, String hostname) {
            this.username = username;
            this.password = password;
            this.hostname = hostname;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getHostname() {
            return hostname;
        }
    }

}
