package one.mixin.android.crypto.attachment;


import java.io.IOException;
import java.io.OutputStream;

public class AttachmentCipherOutputStreamFactory implements OutputStreamFactory {

    private final byte[] key;

    public AttachmentCipherOutputStreamFactory(byte[] key) {
        this.key = key;
    }

    @Override
    public DigestingOutputStream createFor(OutputStream wrap) throws IOException {
        return new AttachmentCipherOutputStream(key, wrap);
    }

    @Override
    public long getCipherTextLength(long plaintextLength) {
        return AttachmentCipherOutputStream.getCipherTextLength(plaintextLength);
    }
}
