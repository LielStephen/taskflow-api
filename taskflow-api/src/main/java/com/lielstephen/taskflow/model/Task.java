package com.lielstephen.taskflow.model;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record Task(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
    public Task {
        id = Objects.requireNonNull(id, "id is required");
        title = normalizeTitle(title);
        description = normalizeDescription(description);
        status = Objects.requireNonNull(status, "status is required");
        priority = Objects.requireNonNull(priority, "priority is required");
        tags = normalizeTags(tags);
        createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static Task createNew(
            String title,
            String description,
            TaskPriority priority,
            LocalDate dueDate,
            List<String> tags,
            Clock clock
    ) {
        Instant now = Instant.now(clock);
        return new Task(
                UUID.randomUUID(),
                title,
                description,
                TaskStatus.BACKLOG,
                priority == null ? TaskPriority.MEDIUM : priority,
                dueDate,
                tags,
                now,
                now
        );
    }

    public Task update(
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            LocalDate dueDate,
            List<String> tags,
            Clock clock
    ) {
        return new Task(
                id,
                title == null ? this.title : title,
                description == null ? this.description : description,
                status == null ? this.status : status,
                priority == null ? this.priority : priority,
                dueDate == null ? this.dueDate : dueDate,
                tags == null ? this.tags : tags,
                createdAt,
                Instant.now(clock)
        );
    }

    public Task clearDueDate(Clock clock) {
        return new Task(id, title, description, status, priority, null, tags, createdAt, Instant.now(clock));
    }

    public Task clearTags(Clock clock) {
        return new Task(id, title, description, status, priority, dueDate, List.of(), createdAt, Instant.now(clock));
    }

    public Task updateStatus(TaskStatus status, Clock clock) {
        return new Task(id, title, description, status, priority, dueDate, tags, createdAt, Instant.now(clock));
    }

    public boolean isOverdue(Clock clock) {
        return dueDate != null && dueDate.isBefore(LocalDate.now(clock)) && status != TaskStatus.COMPLETED;
    }

    public Map<String, Object> toMap(Clock clock) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id.toString());
        data.put("title", title);
        data.put("description", description);
        data.put("status", status.name());
        data.put("priority", priority.name());
        data.put("dueDate", dueDate == null ? null : dueDate.toString());
        data.put("tags", tags);
        data.put("createdAt", createdAt.toString());
        data.put("updatedAt", updatedAt.toString());
        data.put("overdue", isOverdue(clock));
        return data;
    }

    public static Task fromMap(Map<String, Object> data) {
        return new Task(
                UUID.fromString(Objects.toString(data.get("id"))),
                Objects.toString(data.get("title"), ""),
                Objects.toString(data.get("description"), ""),
                TaskStatus.from(Objects.toString(data.get("status"), "")),
                TaskPriority.from(Objects.toString(data.get("priority"), "MEDIUM")),
                data.get("dueDate") == null ? null : LocalDate.parse(Objects.toString(data.get("dueDate"))),
                JsonModelSupport.stringList(data.get("tags")),
                Instant.parse(Objects.toString(data.get("createdAt"))),
                Instant.parse(Objects.toString(data.get("updatedAt")))
        );
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        String normalized = title.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("title must be 120 characters or fewer");
        }
        return normalized;
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        String normalized = description.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("description must be 500 characters or fewer");
        }
        return normalized;
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }
}

