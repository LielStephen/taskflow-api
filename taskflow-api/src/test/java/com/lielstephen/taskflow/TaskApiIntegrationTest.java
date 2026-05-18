package com.lielstephen.taskflow;

import com.lielstephen.taskflow.util.Json;
import com.sun.net.httpserver.HttpServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

public final class TaskApiIntegrationTest {
    private TaskApiIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-05-18T10:15:30Z"), ZoneOffset.UTC);
        Path tempFile = Files.createTempFile("taskflow-api-test", ".json");
        HttpServer server = TaskflowApplication.start(0, tempFile, clock);
        HttpClient client = HttpClient.newHttpClient();
        String baseUrl = "http://localhost:" + server.getAddress().getPort();

        try {
            HttpResponse<String> createResponse = client.send(
                    request(baseUrl + "/api/tasks")
                            .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(Map.of(
                                    "title", "Integrate endpoint test",
                                    "priority", "HIGH",
                                    "tags", List.of("qa", "http")
                            ))))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            assert createResponse.statusCode() == 201 : createResponse.body();
            String taskId = extractTaskId(createResponse.body());

            HttpResponse<String> getResponse = client.send(
                    request(baseUrl + "/api/tasks/" + taskId).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assert getResponse.statusCode() == 200 : getResponse.body();

            HttpResponse<String> patchResponse = client.send(
                    request(baseUrl + "/api/tasks/" + taskId + "/status")
                            .method("PATCH", HttpRequest.BodyPublishers.ofString(Json.stringify(Map.of("status", "COMPLETED"))))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assert patchResponse.statusCode() == 200 : patchResponse.body();

            HttpResponse<String> summaryResponse = client.send(
                    request(baseUrl + "/api/summary").GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assert summaryResponse.statusCode() == 200 : summaryResponse.body();
            assert summaryResponse.body().contains("\"totalTasks\":1");
            assert summaryResponse.body().contains("\"COMPLETED\":1");
        } finally {
            server.stop(0);
        }
    }

    private static HttpRequest.Builder request(String uri) {
        return HttpRequest.newBuilder(URI.create(uri))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    private static String extractTaskId(String responseBody) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) Json.parse(responseBody);
        @SuppressWarnings("unchecked")
        Map<String, Object> task = (Map<String, Object>) payload.get("data");
        return task.get("id").toString();
    }
}

