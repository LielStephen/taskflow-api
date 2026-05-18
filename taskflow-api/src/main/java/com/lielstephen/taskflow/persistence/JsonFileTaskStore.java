package com.lielstephen.taskflow.persistence;

import com.lielstephen.taskflow.model.Task;
import com.lielstephen.taskflow.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JsonFileTaskStore {
    private final Path storagePath;

    public JsonFileTaskStore(Path storagePath) {
        this.storagePath = storagePath;
    }

    public List<Task> load() {
        try {
            if (Files.notExists(storagePath)) {
                ensureParentDirectory();
                return List.of();
            }

            String content = Files.readString(storagePath, StandardCharsets.UTF_8);
            if (content.isBlank()) {
                return List.of();
            }

            Object parsed = Json.parse(content);
            if (!(parsed instanceof List<?> items)) {
                throw new IllegalStateException("Expected a JSON array in " + storagePath);
            }

            List<Task> tasks = new ArrayList<>();
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> rawMap)) {
                    throw new IllegalStateException("Each task entry must be a JSON object");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> taskData = (Map<String, Object>) rawMap;
                tasks.add(Task.fromMap(taskData));
            }
            return tasks;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load tasks from " + storagePath, exception);
        }
    }

    public void save(List<Task> tasks) {
        try {
            ensureParentDirectory();
            Path tempFile = Files.createTempFile(storagePath.getParent(), "taskflow-", ".json");
            List<Map<String, Object>> payload = tasks.stream().map(task -> task.toMap(java.time.Clock.systemUTC())).toList();
            Files.writeString(tempFile, Json.stringify(payload), StandardCharsets.UTF_8);
            Files.move(tempFile, storagePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save tasks to " + storagePath, exception);
        }
    }

    private void ensureParentDirectory() throws IOException {
        Path parent = storagePath.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
    }
}

