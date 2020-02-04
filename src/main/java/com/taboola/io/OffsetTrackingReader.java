package com.taboola.io;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * A wrapping class for {@link Reader} that tracks the number of characters read from the stream.
 * Wraps supplied Reader in a {@link BufferedReader} and therefore supports {@link #mark(int)} and {@link #reset()}.
 */
public class OffsetTrackingReader extends Reader {
    BufferedReader wrapped;
    int offset = 0;
    int markedOffset = 0;

    public OffsetTrackingReader(BufferedReader wrapped) {
        super();
        this.wrapped = wrapped;
    }

    public OffsetTrackingReader(Reader wrapped) {
        this(new BufferedReader(wrapped));
    }

    @Override
    public int read(@NotNull CharBuffer target) throws IOException {
        int amountRead = wrapped.read(target);
        offset += amountRead;
        return amountRead;
    }

    @Override
    public int read() throws IOException {
        int charRead = wrapped.read();
        if (charRead != -1) { offset++; }
        return charRead;
    }

    @Override
    public int read(@NotNull char[] cbuf) throws IOException {
        int amountRead = wrapped.read(cbuf);
        offset += amountRead;
        return amountRead;
    }

    @Override
    public long skip(long n) throws IOException {
        long amountSkipped = wrapped.skip(n);
        offset += amountSkipped;
        return amountSkipped;
    }

    @Override
    public boolean ready() throws IOException {
        return wrapped.ready();
    }

    @Override
    public boolean markSupported() {
        return wrapped.markSupported();
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        markedOffset = offset;
        wrapped.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
        offset = markedOffset;
        wrapped.reset();
    }

    @Override
    public int read(@NotNull char[] cbuf, int off, int len) throws IOException {
        int amountRead = wrapped.read(cbuf, off, len);
        offset += amountRead;
        return amountRead;
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    public int getOffset() {
        return offset;
    }

    public int getMarkedOffset() {
        return markedOffset;
    }
}
