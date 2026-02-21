package com.silicon.agentflow.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志解析工具类
 * 从 OpenROAD 输出中提取关键指标
 */
@Slf4j
public class LogParser {

    // 正则表达式：匹配 "Total Area: 1234.5" 或 "Area: 1234.5"
    private static final Pattern AREA_PATTERN = Pattern.compile(
        "(?:Total\\s+)?Area\\s*[:=]\\s*([0-9]+\\.?[0-9]*)",
        Pattern.CASE_INSENSITIVE
    );

    // 正则表达式：匹配 "Total Power: 0.56" 或 "Power: 0.56"
    private static final Pattern POWER_PATTERN = Pattern.compile(
        "(?:Total\\s+)?Power\\s*[:=]\\s*([0-9]+\\.?[0-9]*)",
        Pattern.CASE_INSENSITIVE
    );

    // 正则表达式：匹配 "Frequency: 1000" 或 "Clock: 1000 MHz"
    private static final Pattern FREQUENCY_PATTERN = Pattern.compile(
        "(?:Frequency|Clock)\\s*[:=]\\s*([0-9]+\\.?[0-9]*)\\s*(?:MHz)?",
        Pattern.CASE_INSENSITIVE
    );

    // 正则表达式：匹配 "Utilization: 65.5%" 或 "Core Utilization: 0.655"
    private static final Pattern UTILIZATION_PATTERN = Pattern.compile(
        "(?:Core\\s+)?Utilization\\s*[:=]\\s*([0-9]+\\.?[0-9]*)\\s*%?",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 从日志中提取面积（um²）
     */
    public static Double extractArea(String log) {
        return extractValue(log, AREA_PATTERN, "Area");
    }

    /**
     * 从日志中提取功耗（mW 或 W）
     */
    public static Double extractPower(String log) {
        return extractValue(log, POWER_PATTERN, "Power");
    }

    /**
     * 从日志中提取频率（MHz）
     */
    public static Double extractFrequency(String log) {
        return extractValue(log, FREQUENCY_PATTERN, "Frequency");
    }

    /**
     * 从日志中提取利用率（%）
     */
    public static Double extractUtilization(String log) {
        return extractValue(log, UTILIZATION_PATTERN, "Utilization");
    }

    /**
     * 通用提取方法
     */
    private static Double extractValue(String logContent, Pattern pattern, String metricName) {
        if (logContent == null || logContent.isEmpty()) {
            log.warn("Log is empty, cannot extract {}", metricName);
            return null;
        }

        Matcher matcher = pattern.matcher(logContent);
        if (matcher.find()) {
            try {
                String valueStr = matcher.group(1);
                double value = Double.parseDouble(valueStr);
                log.info("Extracted {}: {}", metricName, value);
                return value;
            } catch (NumberFormatException e) {
                log.error("Failed to parse {} value: {}", metricName, matcher.group(1), e);
                return null;
            }
        }

        log.warn("{} not found in log", metricName);
        return null;
    }

    /**
     * 解析完整的结果指标
     */
    public static Map<String, Object> parseMetrics(String logContent) {
        Map<String, Object> metrics = new java.util.HashMap<>();

        Double area = extractArea(logContent);
        if (area != null) {
            metrics.put("area_um2", area);
        }

        Double power = extractPower(logContent);
        if (power != null) {
            metrics.put("power_mw", power);
        }

        Double frequency = extractFrequency(logContent);
        if (frequency != null) {
            metrics.put("frequency_mhz", frequency);
        }

        Double utilization = extractUtilization(logContent);
        if (utilization != null) {
            metrics.put("utilization_percent", utilization);
        }

        // 添加时间戳
        metrics.put("parsed_at", java.time.LocalDateTime.now().toString());

        log.info("Parsed metrics: {}", metrics);
        return metrics;
    }

    /**
     * 检查日志中是否包含错误信息
     */
    public static boolean hasErrors(String log) {
        if (log == null) return false;

        String lowerLog = log.toLowerCase();
        return lowerLog.contains("error")
            || lowerLog.contains("failed")
            || lowerLog.contains("exception");
    }
}
