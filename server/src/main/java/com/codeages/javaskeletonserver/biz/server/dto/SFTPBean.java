package com.codeages.javaskeletonserver.biz.server.dto;

import cn.hutool.extra.ssh.Sftp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.schmizz.sshj.sftp.SFTPClient;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SFTPBean {
    private SFTPClient sftp;
    private Long time;
}
