package com.codeages.javaskeletonserver.biz.storage.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileTargetTypeEnum {
    /**
     * 基础文件
     */
    BASE,
    /**
     * 公共文件，不需要校验
     */
    PUBLIC,
    /**
     * 导入文件
     */
    IMPORT,
    /**
     * 导出文件
     */
    EXPORT,
}
