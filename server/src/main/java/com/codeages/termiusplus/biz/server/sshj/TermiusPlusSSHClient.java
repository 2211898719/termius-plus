package com.codeages.termiusplus.biz.server.sshj;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.DirectConnection;

import java.io.IOException;

public class TermiusPlusSSHClient extends SSHClient {
    private DirectConnection proxyDirectConnection;
    private SSHClient proxySshClient;

    @Override
    public void connectVia(DirectConnection directConnection) throws IOException {
        super.connectVia(directConnection);
        this.proxyDirectConnection = directConnection;
    }

    public void connectVia(SSHClient sshClient,String hostname, int port) throws IOException {
        proxySshClient = sshClient;
        DirectConnection sshjDirectConnection = sshClient.newDirectConnection(
                hostname,
                port
                                                                             );
        this.proxyDirectConnection = sshjDirectConnection;
        connectVia(sshjDirectConnection);
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (proxySshClient != null) {
            proxySshClient.close();
        }
        if (proxyDirectConnection != null) {
            proxyDirectConnection.close();
        }
    }
}
