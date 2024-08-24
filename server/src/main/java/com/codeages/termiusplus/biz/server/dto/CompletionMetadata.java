package com.codeages.termiusplus.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletionMetadata {
    private String language;
    private String textAfterCursor;
    private String textBeforeCursor;
}
