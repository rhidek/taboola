package com.taboola.json;

public class JsonParseException extends RuntimeException {
    int offset;

    public JsonParseException(String message, int offset) {
        super(message);
        this.offset = offset;
    }

    public JsonParseException(String message, int offset, Throwable cause) {
        super(message, cause);
        this.offset = offset;
    }

    public Integer getOffset() {
        return offset;
    }
}
