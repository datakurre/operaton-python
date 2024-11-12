package org.operaton.bpm.extension.python;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class GraalPyPolyglotWriter extends OutputStream {
    private volatile Writer writer;

    public GraalPyPolyglotWriter(OutputStreamWriter outputStreamWriter) {
        this.writer = outputStreamWriter;
    }

    @Override
    public void write(int b) throws IOException {
        writer.write(b);
    }

    public Writer getWriter() {
        return writer;
    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }
}
