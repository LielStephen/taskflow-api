package com.lielstephen.taskflow.web;

import com.lielstephen.taskflow.util.Json;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HttpExchangeHelper {
    private HttpExchangeHelper() {
    }

    static Map<String, Object> readJsonObject(HttpExchange exchange) throws IOException {
        try (InputStream body = exchange.getRequestBody()) {
            String payload = new String(body.readAllBytes(), StandardCharsets.UTF_8);
            if (payload.isBlank()) {
                return Map.of();
            }
            return Json.parseObject(payload);
        }
    }

    static void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] body = Json.stringify(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    static void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    static Map<String, Object> responseEnvelope(String message, Object data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", message);
        payload.put("data", data);
        return payload;
    }

    static Map<String, Object> errorEnvelope(String message) {
        return Map.of("error", message);
    }

    static List<String> pathSegments(HttpExchange exchange) {
        return List.of(exchange.getRequestURI().getPath().split("/")).stream()
                .filter(segment -> !segment.isBlank())
                .toList();
    }
}

