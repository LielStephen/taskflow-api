package com.lielstephen.taskflow;

import com.lielstephen.taskflow.model.Task;
import com.lielstephen.taskflow.model.TaskPriority;
import com.lielstephen.taskflow.model.TaskStatus;
import com.lielstephen.taskflow.persistence.JsonFileTaskStore;
import com.lielstephen.taskflow.repository.InMemoryTaskRepository;
import com.lielstephen.taskflow.service.TaskService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TaskServiceTest {
    private TaskServiceTest() {
    }

    public static void main(String[] args) throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-05-18T10:15:30Z"), ZoneOffset.UTC);
        Path tempFile = Files.createTempFile("taskflow-service-test", ".json");
        TaskService service = new TaskService(
                new InMemoryTaskRepository(List.of()),
                new JsonFileTaskStore(tempFile),
                clock
        );

        Task created = service.createTask(Map.of(
                "title", "Ship urgent Java project",
                "description", "Need a polished repo fast",
                "priority", "CRITICAL",
                "dueDate", "2026-05-20",
                "tags", List.of("portfolio", "backend")
        ));

        assert created.priority() == TaskPriority.CRITICAL;
        assert created.status() == TaskStatus.BACKLOG;
        assert created.tags().equals(List.of("portfolio", "backend"));

        Task updated = service.updateTask(created.id(), Map.of(
                "status", "IN_PROGRESS",
                "description", "Repo is being finalized"
        ));

        assert updated.status() == TaskStatus.IN_PROGRESS;
        assert updated.description().equals("Repo is being finalized");

        List<Task> inProgress = service.listTasks(Optional.of(TaskStatus.IN_PROGRESS), Optional.empty(), Optional.empty(), false);
        assert inProgress.size() == 1;

        service.createTask(Map.of(
                "title", "Review API docs",
                "priority", "LOW",
                "dueDate", "2026-05-15",
                "tags", List.of("docs")
        ));

        assert service.listTasks(Optional.empty(), Optional.empty(), Optional.of("docs"), true).size() == 1;
        assert service.buildSummary().totalTasks() == 2;
        assert service.buildSummary().overdueTasks() == 1;
    }
}

