package com.codeages.termiusplus.biz.server.dto;

import com.codeages.termiusplus.biz.util.command.CpuUsage;
import com.codeages.termiusplus.biz.util.command.DiskUsage;
import com.codeages.termiusplus.biz.util.command.NetworkUsage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerRunInfoDTO {
    private ServerDto server;
    private CpuUsage cpuUsage;
    private List<DiskUsage> diskUsages;
    private List<NetworkUsage> networkUsages;
}
