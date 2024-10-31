package com.codeages.termiusplus.biz.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileFindInfoParams {
    @NotNull
    List<String> uuids;
}
