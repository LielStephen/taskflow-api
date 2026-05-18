package com.lielstephen.taskflow.web;

import com.lielstephen.taskflow.model.Task;
import com.lielstephen.taskflow.model.TaskPriority;
import com.lielstephen.taskflow.model.TaskStatus;
import com.lielstephen.taskflow.service.TaskService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public final class TaskHttpHandler implements HttpHandler {
    private final TaskService taskService;
    private final Clock clock;

    public TaskHttpHandler(TaskService taskService, Clock clock) {
        this.taskService = taskService;
        this.clock = clock;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (IllegalArgumentException exception) {
            HttpExchangeHelper.sendJson(exchange, 400, HttpExchangeHelper.errorEnvelope(exception.getMessage()));
        } catch (NoSuchElementException exception) {
            HttpExchangeHelper.sendJson(exchange, 404, HttpExchangeHelper.errorEnvelope(exception.getMessage()));
        } catch (Exception exception) {
            HttpExchangeHelper.sendJson(exchange, 500, HttpExchangeHelper.errorEnvelope("Internal server error"));
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        List<String> segments = HttpExchangeHelper.pathSegments(exchange);

        if (segments.isEmpty()) {
            HttpExchangeHelper.sendJson(exchange, 200, Map.of(
                    "name", "TaskFlow API",
                    "version", "1.0.0",
                    "timestamp", Instant.now(clock).toString()
            ));
            return;
        }

        if (segments.size() == 1 && segments.get(0).equals("health") && method.equals("GET")) {
            HttpExchangeHelper.sendJson(exchange, 200, Map.of(
                    "status", "ok",
                    "timestamp", Instant.now(clock).toString()
            ));
            return;
        }

        if (segments.size() == 2 && segments.get(0).equals("api") && segments.get(1).equals("tasks")) {
            handleTaskCollection(exchange, method);
            return;
        }

        if (segments.size() == 2 && segments.get(0).equals("api") && segments.get(1).equals("summary") && method.equals("GET")) {
            HttpExchangeHelper.sendJson(exchange, 200, HttpExchangeHelper.responseEnvelope("Summary generated", taskService.buildSummary().toMap()));
            return;
        }

        if (segments.size() == 3 && segments.get(0).equals("api") && segments.get(1).equals("tasks")) {
            handleTaskItem(exchange, method, UUID.fromString(segments.get(2)));
            return;
        }

        if (segments.size() == 4 && segments.get(0).equals("api") && segments.get(1).equals("tasks")
                && segments.get(3).equals("status") && method.equals("PATCH")) {
            UUID taskId = UUID.fromString(segments.get(2));
            Map<String, Object> request = HttpExchangeHelper.readJsonObject(exchange);
            Task updated = taskService.updateTaskStatus(taskId, request);
            HttpExchangeHelper.sendJson(exchange, 200, HttpExchangeHelper.responseEnvelope("Task status updated", updated.toMap(clock)));
            return;
        }

        HttpExchangeHelper.sendJson(exchange, 404, HttpExchangeHelper.errorEnvelope("Route not found"));
    }

    private void handleTaskCollection(HttpExchange exchange, String method) throws IOException {
        if (method.equals("GET")) {
            Map<String, String> query = queryParameters(exchange);
            Optional<TaskStatus> status = query.containsKey("status")
                    ? Optional.of(TaskStatus.from(query.get("status")))
                    : Optional.empty();
            Optional<TaskPriority> priority = query.containsKey("priority")
                    ? Optional.of(TaskPriority.from(query.get("priority")))
                    : Optional.empty();
            Optional<String> tag = Optional.ofNullable(query.get("tag"));
            boolean overdueOnly = Boolean.parseBoolean(query.getOrDefault("overdue", "false"));

            List<Map<String, Object>> tasks = taskService.listTasks(status, priority, tag, overdueOnly).stream()
                    .map(task -> task.toMap(clock))
                    .toList();
            HttpExchangeHelper.sendJson(exchange, 200, HttpExchangeHelper.responseEnvelope("Tasks fetched", tasks));
            return;
        }

        if (method.equals("POST")) {
            Map<String, Object> request = HttpExchangeHelper.readJsonObject(exchange);
            Task created = taskService.createTask(request);
            HttpExchangeHelper.sendJson(exchange, 201, HttpExchangeHelper.responseEnvelope("Task created", created.toMap(clock)));
            return;
        }

        HttpExchangeHelper.sendJson(exchange, 405, HttpExchangeHelper.errorEnvelope("Method not allowed"));
    }

    private void handleTaskItem(HttpExchange exchange, String method, UUID taskId) throws IOException {
        switch (method) {
            case "GET" -> {
                Task task = taskService.getTask(taskId);
                HttpExchangeHelper.sendJson(exchange, 200, HttpExchangeHelper.responseEnvelope("Task fetched", task.toMap(clock)));
            }
            case "PUT" -> {
                Map<String, Object> request = HttpExchangeHelper.readJsonObject(exchange);
                Task updated = taskService.updateTask(taskId, request);
                HttpExchangeHelper.sendJson(exchange, 200, HttpExchangeHelper.responseEnvelope("Task updated", updated.toMap(clock)));
            }
            case "DELETE" -> {
                taskService.deleteTask(taskId);
                HttpExchangeHelper.sendNoContent(exchange);
            }
            default -> HttpExchangeHelper.sendJson(exchange, 405, HttpExchangeHelper.errorEnvelope("Method not allowed"));
        }
    }

    private Map<String, String> queryParameters(HttpExchange exchange) {
        Map<String, String> parameters = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return parameters;
        }

        for (String pair : query.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] pieces = pair.split("=", 2);
            String key = decodeComponent(pieces[0]);
            String value = pieces.length > 1 ? decodeComponent(pieces[1]) : "";
            parameters.put(key, value);
        }
        return parameters;
    }

    private String decodeComponent(String value) {
        return value.replace("+", " ");
    }
}

