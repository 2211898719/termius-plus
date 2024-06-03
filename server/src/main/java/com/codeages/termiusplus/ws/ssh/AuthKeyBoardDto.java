package com.codeages.termiusplus.ws.ssh;

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
