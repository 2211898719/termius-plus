package com.codeages.javaskeletonserver.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SFTPServerUploadServerParams {
    private String sourceId;
    private String sourcePath;
    private String targetId;
    private String targetPath;
    private String fileName;
}

