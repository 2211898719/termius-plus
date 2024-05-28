package com.codeages.termiusplus.biz.storage.dto;

import com.codeages.termiusplus.biz.storage.enums.FileTargetTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class FileCreationDTO {
    private String name;
    private String ext;
    private Long size;
    private Long userId;
    private String uri;
    private FileTargetTypeEnum targetType;
    private String uuid;
    private Date uploadTime;
}
