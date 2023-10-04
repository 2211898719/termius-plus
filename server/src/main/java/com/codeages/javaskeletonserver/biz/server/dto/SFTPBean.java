package com.codeages.javaskeletonserver.biz.server.dto;

import cn.hutool.extra.ssh.Sftp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SFTPBean {
    private Sftp sftp;
    private Long time;
}
