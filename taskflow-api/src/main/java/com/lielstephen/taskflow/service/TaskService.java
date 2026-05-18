package com.lielstephen.taskflow.service;

import com.lielstephen.taskflow.model.Task;
import com.lielstephen.taskflow.model.TaskPriority;
import com.lielstephen.taskflow.model.TaskStatus;
import com.lielstephen.taskflow.model.TaskSummary;
import com.lielstephen.taskflow.persistence.JsonFileTaskStore;
import com.lielstephen.taskflow.repository.TaskRepository;
import com.lielstephen.taskflow.util.Json;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class TaskService {
    private final TaskRepository repository;
    private final JsonFileTaskStore taskStore;
    private final Clock clock;

    public TaskService(TaskRepository repository, JsonFileTaskStore taskStore, Clock clock) {
        this.repository = repository;
        this.taskStore = taskStore;
        this.clock = clock;
    }

    public List<Task> listTasks(Optional<TaskStatus> status, Optional<TaskPriority> priority, Optional<String> tag, boolean overdueOnly) {
        Predicate<Task> filters = task -> true;

        if (status.isPresent()) {
            filters = filters.and(task -> task.status() == status.get());
        }
        if (priority.isPresent()) {
            filters = filters.and(task -> task.priority() == priority.get());
        }
        if (tag.isPresent()) {
            String normalizedTag = tag.get().trim().toLowerCase();
            filters = filters.and(task -> task.tags().contains(normalizedTag));
        }
        if (overdueOnly) {
            filters = filters.and(task -> task.isOverdue(clock));
        }

        return repository.findAll().stream().filter(filters).toList();
    }

    public Task getTask(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Task not found: " + id));
    }

    public Task createTask(Map<String, Object> payload) {
        String title = Json.requiredString(payload, "title");
        String description = Json.optionalString(payload, "description");
        TaskPriority priority = TaskPriority.from(Json.optionalString(payload, "priority"));
        LocalDate dueDate = parseDueDate(payload.get("dueDate"));
        List<String> tags = Json.optionalStringList(payload, "tags");

        Task task = Task.createNew(title, description, priority, dueDate, tags, clock);
        Task saved = repository.save(task);
        persist();
        return saved;
    }

    public Task updateTask(UUID id, Map<String, Object> payload) {
        Task existing = getTask(id);

        String title = Json.optionalString(payload, "title");
        String description = Json.optionalNullableString(payload, "description");
        TaskStatus status = payload.containsKey("status") ? TaskStatus.from(Json.requiredString(payload, "status")) : null;
        TaskPriority priority = payload.containsKey("priority") ? TaskPriority.from(Json.requiredString(payload, "priority")) : null;
        LocalDate dueDate = payload.containsKey("dueDate") ? parseDueDate(payload.get("dueDate")) : existing.dueDate();
        List<String> tags = payload.containsKey("tags") ? Json.optionalStringList(payload, "tags") : existing.tags();

        Task updated = existing.update(
                title == null ? existing.title() : title,
                description == null ? existing.description() : description,
                status == null ? existing.status() : status,
                priority == null ? existing.priority() : priority,
                dueDate,
                tags,
                clock
        );

        Task saved = repository.save(updated);
        persist();
        return saved;
    }

    public Task updateTaskStatus(UUID id, Map<String, Object> payload) {
        Task existing = getTask(id);
        Task updated = existing.updateStatus(TaskStatus.from(Json.requiredString(payload, "status")), clock);
        Task saved = repository.save(updated);
        persist();
        return saved;
    }

    public void deleteTask(UUID id) {
        boolean removed = repository.delete(id);
        if (!removed) {
            throw new NoSuchElementException("Task not found: " + id);
        }
        persist();
    }

    public TaskSummary buildSummary() {
        List<Task> tasks = repository.findAll();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        Map<String, Long> byPriority = new LinkedHashMap<>();

        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status.name(), 0L);
        }
        for (TaskPriority priority : TaskPriority.values()) {
            byPriority.put(priority.name(), 0L);
        }

        long overdueTasks = 0L;
        for (Task task : tasks) {
            byStatus.compute(task.status().name(), (key, value) -> value + 1L);
            byPriority.compute(task.priority().name(), (key, value) -> value + 1L);
            if (task.isOverdue(clock)) {
                overdueTasks++;
            }
        }

        return new TaskSummary(tasks.size(), overdueTasks, byStatus, byPriority);
    }

    private LocalDate parseDueDate(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof String stringValue) {
            String candidate = stringValue.trim();
            if (candidate.isEmpty()) {
                return null;
            }
            return LocalDate.parse(candidate);
        }
        throw new IllegalArgumentException("dueDate must be an ISO-8601 date string");
    }

    private void persist() {
        taskStore.save(repository.findAll());
    }
}

