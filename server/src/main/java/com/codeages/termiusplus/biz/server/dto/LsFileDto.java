package com.codeages.termiusplus.biz.server.dto;

import com.codeages.termiusplus.biz.server.enums.LsFileTypeEnum;
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

