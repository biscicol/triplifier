package JenaTools;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

public class StringOutputStream extends OutputStream implements Serializable {

    protected StringBuffer buffer = new StringBuffer();

    @Override
    public void write(byte[] b) throws IOException {
        buffer.append(new String(b));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        buffer.append(new String(b, off, len));
    }

    public void write(int b) throws IOException {
        byte[] byteArr = new byte[] { (byte) b };
        buffer.append(new String(byteArr));
    }

    @Override
    public String toString() {
        return buffer.toString();
    }
 }


