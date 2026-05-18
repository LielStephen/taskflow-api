package com.lielstephen.taskflow.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Json {
    private Json() {
    }

    public static Object parse(String source) {
        return new Parser(source).parse();
    }

    public static Map<String, Object> parseObject(String source) {
        Object parsed = parse(source);
        if (!(parsed instanceof Map<?, ?> object)) {
            throw new IllegalArgumentException("Expected a JSON object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) object;
        return payload;
    }

    public static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        writeJson(builder, value);
        return builder.toString();
    }

    public static String requiredString(Map<String, Object> payload, String fieldName) {
        String value = optionalString(payload, fieldName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    public static String optionalString(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue.trim();
        }
        throw new IllegalArgumentException(fieldName + " must be a string");
    }

    public static String optionalNullableString(Map<String, Object> payload, String fieldName) {
        if (!payload.containsKey(fieldName)) {
            return null;
        }
        Object value = payload.get(fieldName);
        if (value == null) {
            return "";
        }
        if (value instanceof String stringValue) {
            return stringValue.trim();
        }
        throw new IllegalArgumentException(fieldName + " must be a string or null");
    }

    public static List<String> optionalStringList(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(fieldName + " must be an array");
        }
        List<String> values = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String stringValue)) {
                throw new IllegalArgumentException(fieldName + " entries must be strings");
            }
            values.add(stringValue);
        }
        return values;
    }

    private static void writeJson(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String stringValue) {
            writeString(builder, stringValue);
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeString(builder, Objects.toString(entry.getKey()));
                builder.append(':');
                writeJson(builder, entry.getValue());
            }
            builder.append('}');
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            builder.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeJson(builder, item);
            }
            builder.append(']');
            return;
        }
        writeString(builder, value.toString());
    }

    private static void writeString(StringBuilder builder, String value) {
        builder.append('"');
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (current < 0x20) {
                        builder.append(String.format("\\u%04x", (int) current));
                    } else {
                        builder.append(current);
                    }
                }
            }
        }
        builder.append('"');
    }

    private static final class Parser {
        private final String source;
        private int index;

        private Parser(String source) {
            this.source = source == null ? "" : source;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != source.length()) {
                throw error("Unexpected trailing characters");
            }
            return value;
        }

        private Object parseValue() {
            if (index >= source.length()) {
                throw error("Unexpected end of JSON");
            }

            char token = source.charAt(index);
            return switch (token) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> {
                    if (token == '-' || Character.isDigit(token)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected token: " + token);
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();
            Map<String, Object> object = new LinkedHashMap<>();
            if (peek('}')) {
                expect('}');
                return object;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                object.put(key, parseValue());
                skipWhitespace();

                if (peek('}')) {
                    expect('}');
                    return object;
                }

                expect(',');
                skipWhitespace();
            }
        }

        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();
            List<Object> array = new ArrayList<>();
            if (peek(']')) {
                expect(']');
                return array;
            }

            while (true) {
                skipWhitespace();
                array.add(parseValue());
                skipWhitespace();

                if (peek(']')) {
                    expect(']');
                    return array;
                }

                expect(',');
                skipWhitespace();
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();

            while (index < source.length()) {
                char current = source.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current == '\\') {
                    if (index >= source.length()) {
                        throw error("Invalid escape sequence");
                    }
                    char escape = source.charAt(index++);
                    switch (escape) {
                        case '"', '\\', '/' -> builder.append(escape);
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(parseUnicode());
                        default -> throw error("Unsupported escape sequence: \\" + escape);
                    }
                    continue;
                }
                builder.append(current);
            }

            throw error("Unterminated string");
        }

        private char parseUnicode() {
            if (index + 4 > source.length()) {
                throw error("Invalid unicode escape");
            }
            String hex = source.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Boolean parseBoolean() {
            if (source.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (source.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            throw error("Invalid boolean literal");
        }

        private Object parseNull() {
            if (!source.startsWith("null", index)) {
                throw error("Invalid null literal");
            }
            index += 4;
            return null;
        }

        private Number parseNumber() {
            int start = index;
            if (source.charAt(index) == '-') {
                index++;
            }
            consumeDigits();
            boolean decimal = false;
            if (peek('.')) {
                decimal = true;
                index++;
                consumeDigits();
            }
            if (peek('e') || peek('E')) {
                decimal = true;
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                consumeDigits();
            }

            String token = source.substring(start, index);
            return decimal ? Double.parseDouble(token) : Long.parseLong(token);
        }

        private void consumeDigits() {
            if (index >= source.length() || !Character.isDigit(source.charAt(index))) {
                throw error("Expected digit");
            }
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private boolean peek(char expected) {
            return index < source.length() && source.charAt(index) == expected;
        }

        private void expect(char expected) {
            if (!peek(expected)) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at index " + index);
        }
    }
}

