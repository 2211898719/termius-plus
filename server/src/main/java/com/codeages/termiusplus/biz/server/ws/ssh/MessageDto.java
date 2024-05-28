package com.codeages.termiusplus.biz.server.ws.ssh;

import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageDto {
    private EventType event;
    private String data;

    public static MessageDto parse(String message) {
        return JSONUtil.toBean(message, MessageDto.class);
    }
}
