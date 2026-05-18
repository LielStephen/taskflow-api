package com.lielstephen.taskflow.model;

public enum TaskStatus {
    BACKLOG,
    IN_PROGRESS,
    BLOCKED,
    COMPLETED;

    public static TaskStatus from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        return TaskStatus.valueOf(rawValue.trim().toUpperCase());
    }
}

