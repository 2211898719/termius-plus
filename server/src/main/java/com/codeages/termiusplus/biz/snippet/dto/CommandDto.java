package com.codeages.termiusplus.biz.snippet.dto;

import com.codeages.termiusplus.biz.server.dto.ServerDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandDto {
    private Long id;
    private String name;
    private String command;
    private String remark;
    private String serverIds;
    private List<ServerDto> serverDtos;
}

