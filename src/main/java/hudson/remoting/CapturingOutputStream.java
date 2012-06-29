/*
 * The MIT License
 *
 * Copyright 2012 Yahoo!, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * An output stream that remembers a snippet of what was written to it.
 *
 *
 * @author Dean Yu
 */
public class CapturingOutputStream extends OutputStream {
    private RingBuffer<byte[]> capture;
    private OutputStream underlyingStream;

    
    public CapturingOutputStream(OutputStream os) {
        this(os, 1024);
    }
    
    public CapturingOutputStream(OutputStream os, int captureLast) {
        this(os, 0, captureLast);
    }
    
    public CapturingOutputStream(OutputStream os, int captureFirst, int captureLast) {
        this.capture = new RingBuffer<byte[]>(byte[].class, captureFirst, captureLast);
        this.underlyingStream = os;
    }

    @Override
    public void close() throws IOException {
        underlyingStream.close();
    }

    @Override
    public void flush() throws IOException {
        underlyingStream.flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b.length > 0) {
            capture.add(Arrays.copyOf(b, b.length));
        }
        underlyingStream.write(b);
    }
    
    @Override
    public void write(int b) throws IOException {
        byte[] buf = new byte[1];
        buf[0] = (byte)(b & 0x000000ff);
        capture.add(buf);
        underlyingStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len > 0) {
            capture.add(copyOfRange(b, off, off + len));
        }
        underlyingStream.write(b, off, len);
    }

    public void dump(Exception cause) {
        try {
            File dump = File.createTempFile("hudsonRemotingOutputCapture-", ".txt", new File("/tmp"));
            PrintStream stream = new PrintStream(dump);

            Exception e = new Exception("Dump from", cause);
            e.fillInStackTrace();
            e.printStackTrace(stream);
            new ByteArrayRingBufferDumper().dump(capture, stream);
        } catch (IOException e) {
        }
    }

    static byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }
}
