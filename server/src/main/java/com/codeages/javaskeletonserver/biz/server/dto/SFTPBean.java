package com.codeages.javaskeletonserver.biz.server.dto;

import cn.hutool.extra.ssh.Sftp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.StatefulSFTPClient;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SFTPBean {
    private StatefulSFTPClient sftp;
    private Long time;
}
