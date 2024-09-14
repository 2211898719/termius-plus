package com.codeages.termiusplus.biz.util.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetworkUsage {
    private String interfaceName;
    private double receiveBytes;
    private double transmitBytes;
}
