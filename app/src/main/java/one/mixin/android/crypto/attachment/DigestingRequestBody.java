package one.mixin.android.crypto.attachment;


import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.whispersystems.libsignal.util.guava.Preconditions;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DigestingRequestBody extends RequestBody {

  private final InputStream         inputStream;
  private final OutputStreamFactory outputStreamFactory;
  private final String              contentType;
  private final long                contentLength;
  private final PushAttachmentData.ProgressListener progressListener;
  private final CancelationSignal   cancelationSignal;
  private final long                contentStart;

  private byte[] digest;

  public DigestingRequestBody(InputStream inputStream,
                              OutputStreamFactory outputStreamFactory,
                              String contentType, long contentLength,
                              PushAttachmentData.ProgressListener listener,
                              CancelationSignal cancelationSignal,
							  long contentStart)
  {
    Preconditions.checkArgument(contentLength >= contentStart);
    Preconditions.checkArgument(contentStart >= 0);

    this.inputStream         = inputStream;
    this.outputStreamFactory = outputStreamFactory;
    this.contentType         = contentType;
    this.contentLength       = contentLength;
    this.progressListener    = listener;
    this.cancelationSignal   = cancelationSignal;
    this.contentStart        = contentStart;
  }

  @Override
  public MediaType contentType() {
    return MediaType.parse(contentType);
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    FilterOutputStream outputStream = new SkippingOutputStream(contentStart, sink.outputStream());
    if (outputStreamFactory != null) {
      outputStream = outputStreamFactory.createFor(outputStream);
    }
    byte[]                buffer       = new byte[8192];

    int read;
    long total = 0;

    while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
      if (cancelationSignal != null && cancelationSignal.isCanceled()) {
        throw new IOException("Canceled!");
      }

      outputStream.write(buffer, 0, read);
      total += read;

      if (progressListener != null) {
        progressListener.onAttachmentProgress(contentLength, total);
      }
    }

    outputStream.flush();
    if (outputStreamFactory != null) {
      digest =  ((DigestingOutputStream) outputStream).getTransmittedDigest();
    }
  }

  @Override
  public long contentLength() {
    if (contentLength > 0) return contentLength - contentStart;
    else                   return -1;
  }

  public byte[] getTransmittedDigest() {
    return digest;
  }
}

