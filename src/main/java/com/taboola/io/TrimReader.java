package com.taboola.io;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Reader that ignores whitespace outside of quoted Strings.
 * Quotes act as a toggle for ignoring whitespace.
 * Also tracks the stream offset.
 */
public class TrimReader extends OffsetTrackingReader {
    Pattern whitespace = Pattern.compile("\\s");
    boolean ignoreWhitespace = true;

    public TrimReader(Reader reader) {
        super(reader);
    }

    @Override
    public int read() throws IOException {
        int current;
        while ((current = super.read()) != -1) {
            Matcher matcher = whitespace.matcher(String.valueOf((char) current));
            if (!matcher.matches() || !ignoreWhitespace) {
                break;
            }
        }
        return current;
    }

    public void toggleIgnoreWhitespce() {
        ignoreWhitespace = !ignoreWhitespace;
    }

    public boolean isIgnoreWhitespace() {
        return ignoreWhitespace;
    }
}
