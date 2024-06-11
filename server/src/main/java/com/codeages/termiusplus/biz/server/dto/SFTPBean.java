package com.codeages.termiusplus.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.schmizz.sshj.sftp.StatefulSFTPClient;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SFTPBean {
    private StatefulSFTPClient sftp;
    private Long time;
    private boolean active;
}
