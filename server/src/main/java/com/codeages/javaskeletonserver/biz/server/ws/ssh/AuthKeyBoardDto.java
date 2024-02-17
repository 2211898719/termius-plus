package com.codeages.javaskeletonserver.biz.server.ws.ssh;

import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthKeyBoardDto {
    private String serverKey;
    private String password;
}
