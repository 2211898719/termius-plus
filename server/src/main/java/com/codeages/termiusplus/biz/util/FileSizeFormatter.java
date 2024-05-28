package com.codeages.termiusplus.biz.util;

import java.text.DecimalFormat;

public class FileSizeFormatter {
    public static final int ONE_MB = 1024 * 1024; // 1MB的大小
    private static final String[] FILE_SIZE_UNITS = {"B", "KB", "MB", "GB", "TB", "PB"}; // 文件大小单位数组
    private static final int UNIT_SIZE = 1024; // 每个单位的大小

    public static String formatFileSize(long size) {
        if (size <= 0) {
            return "0B"; // 大小为0或负数
        }

        int digitGroups = (int) (Math.log10(size) / Math.log10(UNIT_SIZE));
        if (digitGroups >= FILE_SIZE_UNITS.length) {
            digitGroups = FILE_SIZE_UNITS.length - 1; // 超过最大单位
        }

        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");
        double formattedSize = size / Math.pow(UNIT_SIZE, digitGroups);
        String unit = FILE_SIZE_UNITS[digitGroups];

        return decimalFormat.format(formattedSize) + unit;
    }
}
