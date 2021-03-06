package com.mastfrog.util.streams;

import com.mastfrog.util.file.FileUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * An InputStream-like construct which does not acknowledge the end of files.
 */
public final class ContinuousStringStream implements AutoCloseable {

    private final FileChannel fileChannel;
    private final ByteBuffer readBuffer;

    public ContinuousStringStream(FileChannel fileChannel, int readBufferSizeInBytes) {
        this.fileChannel = fileChannel;
        readBuffer = ByteBuffer.allocateDirect(readBufferSizeInBytes);
    }

    public boolean isOpen() {
        return fileChannel.isOpen();
    }

    int bufferSize() {
        return readBuffer.capacity();
    }

    /**
     * Get the postion the next read will come from
     *
     * @return The position in the file
     * @throws IOException
     */
    public synchronized long position() throws IOException {
        return fileChannel.position();
    }

    /**
     * Change the position
     *
     * @param pos
     * @throws IOException
     */
    public synchronized void position(long pos) throws IOException {
        fileChannel.position(pos);
    }

    @Override
    public synchronized void close() throws IOException {
        fileChannel.close();
    }

    public synchronized int available() throws IOException {
        return (int) (fileChannel.size() - fileChannel.position());
    }

    public synchronized long skip(long l) throws IOException {
        fileChannel.position(fileChannel.position() + l);
        return fileChannel.position();
    }

    boolean hasContent() throws IOException {
        return available() > 0 || readBuffer.position() > 0;
    }

    long size() throws IOException {
        return fileChannel.size();
    }

    /**
     * Decode whatever characters are available into the passed CharBuffer. Note
     * that for multi-byte encodings, CharsetDecoders are stateful, and a
     * previous call could result in being at a byte-position that's part-way
     * through reading a character. Always pass the same decoder unless a
     * decoding error has occurred.
     *
     * @param target The charbuffer to decode results into
     * @param charsetDecoder A decoder for the desired charset
     * @return The result of decoding
     * @throws IOException If something goes wrong
     */
    public synchronized CoderResult decode(CharBuffer target, CharsetDecoder charsetDecoder) throws IOException {
        CoderResult[] r = new CoderResult[1];
        if (readBuffer.position() == readBuffer.capacity()) {
            readBuffer.clear();
        }
        int count = FileUtils.decode(fileChannel, readBuffer, target, charsetDecoder, true, r);
        if (count > 0) {
            target.flip();
        }
        return r[0];
    }

    static String escape(CharBuffer s) {
        s = s.duplicate();
        s.flip();
        StringBuilder sb = new StringBuilder();
        int start = s.position();
        int max = s.length();
        for (int i = start; i < max; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\n");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    static String escape(CharSequence s) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        int max = s.length();
        for (int i = start; i < max; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\n");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    static String escape(char c) {
        switch (c) {
            case '\n':
                return "\\n";
            case '\t':
                return "\\t";
            case 0:
                return "\\0";
            default:
                return "" + c;
        }
    }
}
