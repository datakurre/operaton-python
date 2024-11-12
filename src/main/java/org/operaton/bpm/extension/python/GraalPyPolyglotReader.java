package org.operaton.bpm.extension.python;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class GraalPyPolyglotReader extends InputStream {
    private volatile Reader reader;

    public GraalPyPolyglotReader(InputStreamReader inputStreamReader) {
        this.reader = inputStreamReader;
    }

    @Override
    public int read() throws IOException {
        return reader.read();
    }

    public Reader getReader() {
        return reader;
    }

    public void setReader(Reader reader) {
        this.reader = reader;
    }
}
