package one.mixin.android.crypto;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import one.mixin.android.crypto.attachment.CancelationSignal;
import one.mixin.android.crypto.attachment.DigestingRequestBody;
import one.mixin.android.crypto.attachment.OutputStreamFactory;
import one.mixin.android.crypto.attachment.PushAttachmentData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

public class Util {

    public static byte[] join(byte[]... input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (byte[] part : input) {
                baos.write(part);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[][] split(byte[] input, int firstLength, int secondLength) {
        byte[][] parts = new byte[2][];

        parts[0] = new byte[firstLength];
        System.arraycopy(input, 0, parts[0], 0, firstLength);

        parts[1] = new byte[secondLength];
        System.arraycopy(input, firstLength, parts[1], 0, secondLength);

        return parts;
    }

    public static byte[] trim(byte[] input, int length) {
        byte[] result = new byte[length];
        System.arraycopy(input, 0, result, 0, result.length);

        return result;
    }

    public static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }


    public static byte[] getSecretBytes(int size) {
        byte[] secret = new byte[size];
        new SecureRandom().nextBytes(secret);
        return secret;
    }

    public static void readFully(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;

        for (; ; ) {
            int read = in.read(buffer, offset, buffer.length - offset);

            if (read + offset < buffer.length) offset += read;
            else return;
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int read;

        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }

        in.close();
        out.close();
    }

    public static int toIntExact(long value) {
        if ((int) value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) value;
    }

    public static byte[] uploadAttachment(String url, InputStream data, long dataSize, OutputStreamFactory outputStreamFactory, PushAttachmentData.ProgressListener listener, CancelationSignal cancelationSignal) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().build();
        DigestingRequestBody requestBody = new DigestingRequestBody(data, outputStreamFactory, "application/octet-stream", dataSize, listener, cancelationSignal, 0);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("x-amz-acl", "public-read")
                .addHeader("Connection", "close")
                .put(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            return requestBody.getTransmittedDigest();
        }
        return null;
    }
}
