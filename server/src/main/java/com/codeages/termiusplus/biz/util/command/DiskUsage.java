package com.codeages.termiusplus.biz.util.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiskUsage {
    private String filesystem;
    private String size;
    private String used;
    private String available;
    private String use;
    private String mountedOn;
}
