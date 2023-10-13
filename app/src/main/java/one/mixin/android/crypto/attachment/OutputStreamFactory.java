package one.mixin.android.crypto.attachment;


import java.io.IOException;
import java.io.OutputStream;

public interface OutputStreamFactory {

  public DigestingOutputStream createFor(OutputStream wrap) throws IOException;

}
