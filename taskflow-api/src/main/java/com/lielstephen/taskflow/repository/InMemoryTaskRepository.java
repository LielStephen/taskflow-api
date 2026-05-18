package com.lielstephen.taskflow.repository;

import com.lielstephen.taskflow.model.Task;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryTaskRepository implements TaskRepository {
    private static final Comparator<Task> TASK_ORDER = Comparator
            .comparingInt((Task task) -> task.priority().rank()).reversed()
            .thenComparing(task -> task.dueDate(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Task::updatedAt, Comparator.reverseOrder());

    private final ConcurrentMap<UUID, Task> tasks = new ConcurrentHashMap<>();

    public InMemoryTaskRepository(Collection<Task> seedData) {
        for (Task task : seedData) {
            tasks.put(task.id(), task);
        }
    }

    @Override
    public List<Task> findAll() {
        return tasks.values().stream().sorted(TASK_ORDER).toList();
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public Task save(Task task) {
        tasks.put(task.id(), task);
        return task;
    }

    @Override
    public boolean delete(UUID id) {
        return tasks.remove(id) != null;
    }
}

