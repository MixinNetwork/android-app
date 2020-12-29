package one.mixin.android.crypto.attachment;


import java.io.IOException;
import java.io.OutputStream;

public interface OutputStreamFactory {

    DigestingOutputStream createFor(OutputStream wrap) throws IOException;

    long getCipherTextLength(long plaintextLength);
}
