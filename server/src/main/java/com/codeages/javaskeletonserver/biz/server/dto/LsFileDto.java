package com.codeages.javaskeletonserver.biz.server.dto;

import com.codeages.javaskeletonserver.biz.server.enums.LsFileTypeEnum;
import com.codeages.javaskeletonserver.biz.server.enums.ProxyTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LsFileDto {
    private String fileName;
    private LsFileTypeEnum type;
}

