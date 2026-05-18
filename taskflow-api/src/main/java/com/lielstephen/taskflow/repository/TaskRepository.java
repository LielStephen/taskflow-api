package com.lielstephen.taskflow.repository;

import com.lielstephen.taskflow.model.Task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    List<Task> findAll();

    Optional<Task> findById(UUID id);

    Task save(Task task);

    boolean delete(UUID id);
}

