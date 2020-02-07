package com.taboola.json;

import com.taboola.io.TrimReader;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.math.NumberUtils.isCreatable;
import static org.apache.commons.lang3.math.NumberUtils.isDigits;

/**
 * Parses JSON into the corresponding nested data structure.
 * Limitations:  limited to two byte character encodings (i.e. won't support 3 & 4 byte UTF character codes).
 * Warnings: If you expect to parse extremely nested JSON documents make sure you set the stack size
 * accordingly (e.g. -Xss10m).  This implementation is recursive.
 */
@Slf4j
public class JsonParser {
    /**
     * A wrapper around {@link #parse(Reader)} that supports String
     *
     * @param json A String containing JSON
     * @return A Map representing the JSON in the supplied String
     * @throws JsonParseException When the supplied string contains invalid JSON
     */
    public Map<String, Object> parse(String json) throws JsonParseException {
        Objects.requireNonNull(json);

        return parse(new StringReader(json));
    }

    /**
     * Deserializes the JSON character stream into a Map.
     *
     * @param reader An open character stream.
     * @return A Map representing the JSON in the supplied character stream
     * @throws JsonParseException When the supplied character stream contains invalid JSON
     */
    public Map<String, Object> parse(Reader reader) throws JsonParseException {
        Objects.requireNonNull(reader);

        TrimReader trimReader = new TrimReader(reader);
        try {
            int current = trimReader.read();
            if (current == -1) {
                throw new JsonParseException("JSON is empty.", 0);
            } else if ((char) current != '{') {
                throw new JsonParseException("JSON must begin with an Object.", trimReader.getOffset() - 1);
            }
            return nextObject(trimReader);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new JsonParseException("IO error.", trimReader.getOffset() - 1, e);
        }
    }

    private Map<String, Object> nextKv(TrimReader reader) throws JsonParseException, IOException {
        Map<String, Object> result = new HashMap<>();

        String key = nextString(reader);

        int current = reader.read();
        if (current == -1 || (char) current != ':') {
            throw unexpectedChar(reader, (char) current);
        }

        result.put(key, nextValue(reader));
        return result;
    }

    private Map<String, Object> nextObject(TrimReader reader) throws JsonParseException, IOException {
        int startingOffset = reader.getOffset() - 1; // -1 since start of object is consumed prior

        Map<String, Object> result = new HashMap<>();
        int current;
        while ((current = reader.read()) != -1) {
            switch ((char) current) {
                case '"':
                    result.putAll(nextKv(reader));
                    break;
                case ',':
                    break; //TODO detect degenerate cases e.g. { ,,, "key":"val",, }
                case '}':
                    return result;
                default:
                    throw unexpectedChar(reader, (char) current);
            }
        }
        throw noClosingToken(reader, "object", startingOffset);
    }

    private List<Object> nextArray(TrimReader reader) throws JsonParseException, IOException {
        int startingOffset = reader.getOffset() - 1; // -1 since start of object is consumed prior
        reader.mark(1);
        List<Object> result = new ArrayList<>();
        int current;
        while ((current = reader.read()) != -1) {
            switch ((char) current) {
                case ']':
                    return result;
                case ',':
                    result.add(nextValue(reader));
                    break;
                default:
                    reader.reset(); //undo reading the first char of the next token
                    result.add(nextValue(reader));
            }
            reader.mark(1);
        }
        throw noClosingToken(reader, "array", startingOffset);
    }

    private Object nextValue(TrimReader reader) throws JsonParseException, IOException {
        Object value;
        reader.mark(1);
        int current = reader.read();
        if (current == -1) {
            throw unexpectedEnd(reader);
        }
        switch ((char) current) {
            case '"':
                log.debug("Detected String type.");
                value = nextString(reader);
                break;
            case 'n':
                log.debug("Detected null type.");
                value = nextNull(reader);
                break;
            case 'f':
                log.debug("Detected Boolean type.");
                value = nextFalse(reader);
                break;
            case 't':
                log.debug("Detected Boolean type.");
                value = nextTrue(reader);
                break;
            case '{':
                log.debug("Detected Object type.");
                value = nextObject(reader);
                break;
            case '[':
                log.debug("Detected Array type.");
                value = nextArray(reader);
                break;
            default:
                if (isDigits(String.valueOf((char) current))) {
                    log.debug("Detected Numeric type.");
                    reader.reset();
                    value = nextNumber(reader);
                } else {
                    throw unexpectedChar(reader, (char) current);
                }
        }
        return value;
    }

    private String nextString(TrimReader reader) throws JsonParseException, IOException {
        int startingOffset = reader.getOffset() - 1; // -1 since start of object is consumed prior
        log.debug("Toggling whitespace on.");
        reader.toggleIgnoreWhitespce();
        StringBuilder sb = new StringBuilder();
        int current;
        while ((current = reader.read()) != -1) {
            if ((char) current == '"') { //TODO handle escaped quotes
                log.debug("Toggling whitespace off.");
                reader.toggleIgnoreWhitespce();
                return sb.toString();
            }
            sb.append((char) current);
        }
        log.debug("Toggling whitespace off.");
        reader.toggleIgnoreWhitespce();
        throw noClosingToken(reader, "string", startingOffset);
    }

    private Object nextNull(TrimReader reader) throws JsonParseException, IOException {
        int startingOffset = reader.getOffset() - 1;
        String found = readCountChars(reader, 3);
        if (found.equals("ull")) {
            return null;
        }
        throw new JsonParseException(String.format("Expected 'null' but got 'n%s'.", found), startingOffset);
    }

    private Boolean nextTrue(TrimReader reader) throws JsonParseException, IOException {
        int startingOffset = reader.getOffset() - 1;
        String found = readCountChars(reader, 3);
        if (found.equals("rue")) {
            return true;
        }
        throw new JsonParseException(String.format("Expected 'true' but got 't%s'.", found), startingOffset);
    }

    private Boolean nextFalse(TrimReader reader) throws JsonParseException, IOException {
        int startingOffset = reader.getOffset() - 1;
        String found = readCountChars(reader, 4);
        if (found.equals("alse")) {
            return false;
        }
        throw new JsonParseException(String.format("Expected 'false' but got 'f%s'.", found), startingOffset);
    }

    private String readCountChars(TrimReader reader, int count) throws JsonParseException, IOException {
        StringBuilder sb = new StringBuilder();
        int current;
        for (int i = 0; i < count; i++) {
            current = reader.read();
            if (current == -1) {
                throw unexpectedEnd(reader);
            }
            sb.append((char) current);
        }
        return sb.toString();
    }

    private BigDecimal nextNumber(TrimReader reader) throws JsonParseException, IOException {
        StringBuilder sb = new StringBuilder();
        int current;
        while ((current = reader.read()) != -1) {
            switch ((char) current) {
                case 'e':
                case 'E':
                case '+':
                case '-':
                case '.':
                    sb.append((char) current);
                    break;
                case ',':
                case '}':
                case ']':
                    reader.reset();
                    if (isCreatable(sb.toString())) {
                        return new BigDecimal(sb.toString());
                    }
                    else {
                        throw new JsonParseException(String.format("Invalid numeric type: '%s'.", sb.toString()), reader.getOffset());
                    }
                default:
                    if (isDigits(String.valueOf((char) current))) {
                        sb.append((char) current);
                    } else {
                        throw unexpectedChar(reader, (char) current);
                    }
            }
            reader.mark(1);
        }

        throw unexpectedEnd(reader);
    }

    @NotNull
    private JsonParseException unexpectedChar(TrimReader reader, char current) {
        return new JsonParseException(String.format("Encountered unexpected token: '%s'.", current),
                                      reader.getOffset() - 1);
    }

    @NotNull
    private JsonParseException unexpectedEnd(TrimReader reader) {
        return new JsonParseException("Encountered unexpected end of stream.", reader.getOffset() - 1);
    }

    @NotNull
    private JsonParseException noClosingToken(TrimReader reader, String type, int startingOffset) {
        return new JsonParseException(String.format("No closing token for %s started at offset %d.",
                                                    type,
                                                    startingOffset), reader.getOffset() - 1);
    }
}
