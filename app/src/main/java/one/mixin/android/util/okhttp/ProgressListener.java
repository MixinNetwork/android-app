package one.mixin.android.util.okhttp;

public interface ProgressListener {
    void update(long bytesRead, long contentLength, boolean done);
}