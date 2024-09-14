package com.codeages.termiusplus.biz.util.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CpuUsage {
    private Double us;
    private Double sy;
    private Double id;
}
