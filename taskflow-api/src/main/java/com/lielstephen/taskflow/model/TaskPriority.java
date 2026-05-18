package com.lielstephen.taskflow.model;

public enum TaskPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int rank;

    TaskPriority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public static TaskPriority from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return MEDIUM;
        }
        return TaskPriority.valueOf(rawValue.trim().toUpperCase());
    }
}

