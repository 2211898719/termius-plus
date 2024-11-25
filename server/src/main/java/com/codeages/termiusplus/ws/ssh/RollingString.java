package com.codeages.termiusplus.ws.ssh;

public class RollingString {
    private final StringBuilder stringBuilder;
    private int maxLength = 2048;

    public RollingString() {
        this.stringBuilder = new StringBuilder();
    }

    public RollingString(int maxLength) {
        this.stringBuilder = new StringBuilder();
        this.maxLength = maxLength;
    }

    public void append(String str) {
        if (str == null) return;

        // 计算新的总长度
        int newLength = stringBuilder.length() + str.length();

        // 添加新字符串
        stringBuilder.append(str);

        // 如果总长度超过最大限制，进行删除
        if (newLength > maxLength) {
            // 计算需要删除的字符数
            int charsToDelete = newLength - maxLength;
            // 删除开头的字符
            stringBuilder.delete(0, charsToDelete);
        }
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }
}
