package com.lielstephen.taskflow.model;

import java.util.List;
import java.util.Objects;

final class JsonModelSupport {
    private JsonModelSupport() {
    }

    static List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("tags must be an array");
        }
        return list.stream()
                .map(item -> Objects.toString(item, "").trim())
                .filter(item -> !item.isBlank())
                .toList();
    }
}

