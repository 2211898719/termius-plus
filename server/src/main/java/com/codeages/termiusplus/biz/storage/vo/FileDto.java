package com.codeages.termiusplus.biz.storage.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDto {
    private String name;
    private String ext;
    private Long size;
    private Long userId;
    private String username;
    private String uuid;
    private String uri;
    private Date uploadTime;
}
