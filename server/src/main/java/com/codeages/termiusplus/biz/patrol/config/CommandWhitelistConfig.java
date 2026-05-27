package com.codeages.termiusplus.biz.patrol.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "patrol")
public class CommandWhitelistConfig {

    private List<WhitelistEntry> whitelist = new ArrayList<>();
    private ScheduleConfig schedule = new ScheduleConfig();

    @Data
    public static class WhitelistEntry {
        private String pattern;
        private boolean autoExecute;
    }

    @Data
    public static class ScheduleConfig {
        private String cron = "0 0 2,14 * * ?";
    }

    public boolean shouldAutoExecute(String command) {
        for (WhitelistEntry entry : whitelist) {
            if (matchesPattern(entry.getPattern(), command)) {
                return entry.isAutoExecute();
            }
        }
        return false;
    }

    private boolean matchesPattern(String pattern, String command) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        return command.trim().matches(regex);
    }
}
