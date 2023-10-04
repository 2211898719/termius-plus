package com.codeages.javaskeletonserver.biz.server.dto;

import com.codeages.javaskeletonserver.biz.server.enums.LsFileTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SFTPParams {
    private String remotePath;
    private String newRemotePath;
}

