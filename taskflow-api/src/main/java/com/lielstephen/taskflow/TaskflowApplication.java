package com.lielstephen.taskflow;

import com.lielstephen.taskflow.persistence.JsonFileTaskStore;
import com.lielstephen.taskflow.repository.InMemoryTaskRepository;
import com.lielstephen.taskflow.service.TaskService;
import com.lielstephen.taskflow.web.TaskHttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class TaskflowApplication {
    private TaskflowApplication() {
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Path storagePath = Path.of(System.getenv().getOrDefault("TASKFLOW_DATA_FILE", "data/tasks.json"));
        HttpServer server = start(port, storagePath, Clock.systemUTC());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(1)));
        System.out.println("TaskFlow API started on http://localhost:" + server.getAddress().getPort());
    }

    public static HttpServer start(int port, Path storagePath, Clock clock) throws IOException {
        JsonFileTaskStore taskStore = new JsonFileTaskStore(storagePath);
        InMemoryTaskRepository repository = new InMemoryTaskRepository(taskStore.load());
        TaskService taskService = new TaskService(repository, taskStore, clock);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new TaskHttpHandler(taskService, clock));
        server.setExecutor(newHttpExecutor());
        server.start();
        return server;
    }

    private static ExecutorService newHttpExecutor() {
        AtomicInteger threadCounter = new AtomicInteger(1);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "taskflow-http-" + threadCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(8, factory);
    }
}
