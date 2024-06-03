package com.codeages.termiusplus.ws.ssh;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ResizeDto {
    private int cols;
    private int rows;
    private int width;
    private int height;
}
