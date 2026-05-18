package com.lielstephen.taskflow.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record TaskSummary(
        int totalTasks,
        long overdueTasks,
        Map<String, Long> byStatus,
        Map<String, Long> byPriority
) {
    public Map<String, Object> toMap() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTasks", totalTasks);
        summary.put("overdueTasks", overdueTasks);
        summary.put("byStatus", byStatus);
        summary.put("byPriority", byPriority);
        return summary;
    }
}

