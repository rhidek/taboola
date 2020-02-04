package com.taboola.json;

import com.taboola.io.OffsetTrackingReader;
import com.taboola.io.TrimReader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

        Map<String, Object> result = new HashMap<>();
        TrimReader trimReader = new TrimReader(reader);
        try {
            int current = trimReader.read();
            if (current == -1) {
                throw new JsonParseException("JSON is empty", 0);
            } else if ((char) current != '{') {
                throw new JsonParseException("JSON must begin with an Object", 0);
            }
            current = trimReader.read();
            if (current != -1) {
                switch ((char) current) {
                    case '"':
                        result.putAll(nextKv(trimReader));
                        break;
                    case ',':
                    case '}':
                        break;
                    default:
                        throw new JsonParseException(String.format("Encountered unexpected token: %s", (char) current),
                                                     trimReader.getOffset());
                }
            } else {
                throw new JsonParseException("Encountered unexpected end of Object", trimReader.getOffset());
            }
        } catch (IOException e) {
            //Shouldn't ever hit this block
            log.error(e.getMessage(), e);
            throw new JsonParseException("Invalid character received", trimReader.getOffset());
        }
        return result;
    }

    private Map<String, Object> nextKv(OffsetTrackingReader reader) throws IOException {
        String key = nextString(reader);
        int current = reader.read();
        if (current == -1 || (char) current != ':') {
            throw new JsonParseException("TODO", reader.getOffset()); //TODO
        }
        current = reader.read();
        if (current == -1) {
            //TODO
        }

        String value = nextString(reader);
        Map<String, Object> result = new HashMap<>();
        result.put(key, value);
        return result;
    }

    private String nextString(OffsetTrackingReader reader) throws JsonParseException, IOException {
        StringBuilder sb = new StringBuilder();
        int current = 0;
        while ((current = reader.read()) != -1) {
            if ((char) current == '"') {
                break;
            }
            sb.append((char) current);
        }
        if ((char) current == '"') {
            return sb.toString();
        }
//        else {
//            throw new JsonParseException(String.format("Unexpected end of key. Expected '\"' but found '%s'",
//                                                       (char) current), reader.offset);
//        }
        return null; //TODO proper exception handling
    }
}
