package com.codeages.termiusplus.biz.server.ws.ssh;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class SftpServerUploadServerProgressDto {
    private String sourceServerName;
    private String sourceFileName;
    private String targetServerName;
    private String targetFileName;
    private Long progress;
    private String sourceId;
    private String targetId;
}
