package com.codeages.termiusplus.biz.storage.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileVo {
    private String name;
    private String ext;
    private Long size;
    private Long userId;
    private Long userName;
    private String uuid;
    private String uri;
    private Date uploadTime;
}
