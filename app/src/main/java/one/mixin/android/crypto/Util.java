package one.mixin.android.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import one.mixin.android.crypto.attachment.DigestingOutputStream;
import one.mixin.android.crypto.attachment.OutputStreamFactory;
import one.mixin.android.crypto.attachment.PushAttachmentData;

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

    public static String getSecret(int size) {
        byte[] secret = getSecretBytes(size);
        return Base64.encodeBytes(secret);
    }

    public static byte[] getSecretBytes(int size) {
        byte[] secret = new byte[size];
        new SecureRandom().nextBytes(secret);
        return secret;
    }

    public static byte[] getRandomLengthBytes(int maxSize) {
        SecureRandom secureRandom = new SecureRandom();
        byte[]       result       = new byte[secureRandom.nextInt(maxSize) + 1];
        secureRandom.nextBytes(result);
        return result;
    }

    public static byte[] getRequestNonce(String data) {
        SecureRandom random = new SecureRandom();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        try {
            byteStream.write(bytes);
            byteStream.write(data.getBytes());
        } catch (IOException e) {
            return null;
        }
        return byteStream.toByteArray();
    }

    public static String readFully(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer              = new byte[4096];
        int read;

        while ((read = in.read(buffer)) != -1) {
            bout.write(buffer, 0, read);
        }

        in.close();

        return new String(bout.toByteArray());
    }

    public static void readFully(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;

        for (;;) {
            int read = in.read(buffer, offset, buffer.length - offset);

            if (read + offset < buffer.length) offset += read;
            else                		           return;
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
        if ((int)value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int)value;
    }

    @SafeVarargs
    public static <T> List<T> immutableList(T... elements) {
        return Collections.unmodifiableList(Arrays.asList(elements.clone()));
    }


    public static byte[] uploadAttachment(String method, HttpsURLConnection connection, InputStream data,
                                          long dataSize, OutputStreamFactory outputStreamFactory, PushAttachmentData.ProgressListener listener)
            throws IOException {

        connection.setDoOutput(true);

        if (dataSize > 0) {
            connection.setFixedLengthStreamingMode(Util.toIntExact(dataSize));
        } else {
            connection.setChunkedStreamingMode(0);
        }

        connection.setRequestMethod(method);
        connection.setRequestProperty("x-amz-acl", "public-read");
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setRequestProperty("Connection", "close");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.connect();

        try {
            OutputStream out;
            if (outputStreamFactory == null) {
                out = connection.getOutputStream();
            } else {
                out = outputStreamFactory.createFor(connection.getOutputStream());
            }
            byte[] buffer = new byte[8192];
            int read, written = 0;

            while ((read = data.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                written += read;

                if (listener != null) {
                    listener.onAttachmentProgress(dataSize, written);
                }
            }

            out.flush();
            data.close();
            out.close();

            if (connection.getResponseCode() != 200) {
                throw new IOException("Bad response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
            }

            if (outputStreamFactory == null) {
                return null;
            } else {
                return ((DigestingOutputStream) out).getTransmittedDigest();
            }
        } finally {
            connection.disconnect();
        }
    }
}
