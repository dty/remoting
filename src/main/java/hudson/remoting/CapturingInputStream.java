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
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;


/**
 * An input stream that captures a snippet of what was read from it.
 * 
 * @author Dean Yu
 */
public class CapturingInputStream extends InputStream {
    private RingBuffer<byte[]> capture;
    private InputStream underlyingStream;

    public CapturingInputStream(InputStream is) {
        this(is, 1024);
    }

    public CapturingInputStream(InputStream is, int captureLast) {
        this(is, 0, captureLast);
    }

    public CapturingInputStream(InputStream is, int captureFirst, int captureLast) {
        this.capture = new RingBuffer<byte[]>(byte[].class, captureFirst, captureLast);
        this.underlyingStream = is;
    }

    @Override
    public int available() throws IOException {
        return underlyingStream.available();
    }

    @Override
    public void close() throws IOException {
        underlyingStream.close();
    }

    @Override
    public void mark(int readLimit) {
        underlyingStream.mark(readLimit);
    }

    @Override
    public boolean markSupported() {
        return underlyingStream.markSupported();
    }

    @Override
    public int read(byte[] b) throws IOException {
        int numRead = underlyingStream.read(b);
        if (numRead > 0) {
            capture(b, 0, numRead);
        }
        return numRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int numRead = underlyingStream.read(b, off, len);
        if (numRead > 0) {
            capture(b, off, numRead);
        }

        return numRead;
    }

    @Override
    public int read() throws IOException {
        int data = underlyingStream.read();
        if (data > 0) {
            byte[] buf = new byte[1];
            buf[0] = (byte) data;

            capture(buf, 0, 1);
        }
        return data;
    }

    void captureRest() {
        try {
            byte[] marker = { (byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef };
            capture(marker, 0, 4);

            int rest = available();
            if (rest > 0) {
                byte[] buf = new byte[rest];
                read(buf);
            }
        } catch (IOException e) {
            
        }
    }

    public void dump(Exception cause) {
        try {
            File dump = File.createTempFile("hudsonRemotingInputCapture-", ".txt", new File("/tmp"));
            PrintStream stream = new PrintStream(dump);

            Exception e = new Exception("Dump from", cause);
            e.fillInStackTrace();
            e.printStackTrace(stream);

            new ByteArrayRingBufferDumper().dump(capture, stream);
        } catch (IOException e) {
        }
    }
    
    private void capture(byte[] buf, int off, int len) throws IOException {
        capture.add(CapturingOutputStream.copyOfRange(buf, off, off + len));
    }
}
