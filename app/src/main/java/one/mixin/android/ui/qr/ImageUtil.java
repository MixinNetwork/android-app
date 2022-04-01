/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package one.mixin.android.ui.qr;

import android.graphics.*;
import android.util.Size;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;
import timber.log.Timber;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Utility class for image related operations.
 *
 */
final class ImageUtil {
    private static final String TAG = "ImageUtil";

    private ImageUtil() {
    }

    /** {@link android.media.Image} to JPEG byte array. */
    public static byte[] imageToJpegByteArray(ImageProxy image) throws CodecFailedException {
        byte[] data = null;
        if (image.getFormat() == ImageFormat.JPEG) {
            data = jpegImageToJpegByteArray(image);
        } else if (image.getFormat() == ImageFormat.YUV_420_888) {
            data = yuvImageToJpegByteArray(image);
        } else {
            Timber.tag(TAG).w("Unrecognized image format: %s", image.getFormat());
        }
        return data;
    }

    /** Crops byte array with given {@link android.graphics.Rect}. */
    public static byte[] cropByteArray(byte[] data, Rect cropRect) throws CodecFailedException {
        if (cropRect == null) {
            return data;
        }

        Bitmap bitmap = null;
        try {
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(data, 0, data.length,
                    false);
            bitmap = decoder.decodeRegion(cropRect, new BitmapFactory.Options());
            decoder.recycle();
        } catch (IllegalArgumentException e) {
            throw new CodecFailedException("Decode byte array failed with illegal argument." + e,
                    CodecFailedException.FailureType.DECODE_FAILED);
        } catch (IOException e) {
            throw new CodecFailedException("Decode byte array failed.",
                    CodecFailedException.FailureType.DECODE_FAILED);
        }

        if (bitmap == null) {
            throw new CodecFailedException("Decode byte array failed.",
                    CodecFailedException.FailureType.DECODE_FAILED);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        if (!success) {
            throw new CodecFailedException("Encode bitmap failed.",
                    CodecFailedException.FailureType.ENCODE_FAILED);
        }
        bitmap.recycle();

        return out.toByteArray();
    }

    private static byte[] nv21ToJpeg(byte[] nv21, int width, int height, @Nullable Rect cropRect)
            throws CodecFailedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        boolean success =
                yuv.compressToJpeg(
                        cropRect == null ? new Rect(0, 0, width, height) : cropRect, 100, out);
        if (!success) {
            throw new CodecFailedException("YuvImage failed to encode jpeg.",
                    CodecFailedException.FailureType.ENCODE_FAILED);
        }
        return out.toByteArray();
    }

    private static byte[] yuv_420_888toNv21(ImageProxy image) {
        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = image.getPlanes()[1];
        ImageProxy.PlaneProxy vPlane = image.getPlanes()[2];

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();
        yBuffer.rewind();
        uBuffer.rewind();
        vBuffer.rewind();

        int ySize = yBuffer.remaining();

        int position = 0;
        // TODO(b/115743986): Pull these bytes from a pool instead of allocating for every image.
        byte[] nv21 = new byte[ySize + (image.getWidth() * image.getHeight() / 2)];

        // Add the full y buffer to the array. If rowStride > 1, some padding may be skipped.
        for (int row = 0; row < image.getHeight(); row++) {
            yBuffer.get(nv21, position, image.getWidth());
            position += image.getWidth();
            yBuffer.position(
                    Math.min(ySize, yBuffer.position() - image.getWidth() + yPlane.getRowStride()));
        }

        int chromaHeight = image.getHeight() / 2;
        int chromaWidth = image.getWidth() / 2;
        int vRowStride = vPlane.getRowStride();
        int uRowStride = uPlane.getRowStride();
        int vPixelStride = vPlane.getPixelStride();
        int uPixelStride = uPlane.getPixelStride();

        // Interleave the u and v frames, filling up the rest of the buffer. Use two line buffers to
        // perform faster bulk gets from the byte buffers.
        byte[] vLineBuffer = new byte[vRowStride];
        byte[] uLineBuffer = new byte[uRowStride];
        for (int row = 0; row < chromaHeight; row++) {
            vBuffer.get(vLineBuffer, 0, Math.min(vRowStride, vBuffer.remaining()));
            uBuffer.get(uLineBuffer, 0, Math.min(uRowStride, uBuffer.remaining()));
            int vLineBufferPosition = 0;
            int uLineBufferPosition = 0;
            for (int col = 0; col < chromaWidth; col++) {
                nv21[position++] = vLineBuffer[vLineBufferPosition];
                nv21[position++] = uLineBuffer[uLineBufferPosition];
                vLineBufferPosition += vPixelStride;
                uLineBufferPosition += uPixelStride;
            }
        }

        return nv21;
    }

    private static boolean shouldCropImage(ImageProxy image) {
        Size sourceSize = new Size(image.getWidth(), image.getHeight());
        Size targetSize = new Size(image.getCropRect().width(), image.getCropRect().height());

        return !targetSize.equals(sourceSize);
    }

    private static byte[] jpegImageToJpegByteArray(ImageProxy image) throws CodecFailedException {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] data = new byte[buffer.capacity()];
        buffer.get(data);
        if (shouldCropImage(image)) {
            data = cropByteArray(data, image.getCropRect());
        }
        return data;
    }

    private static byte[] yuvImageToJpegByteArray(ImageProxy image)
            throws CodecFailedException {
        return ImageUtil.nv21ToJpeg(
                ImageUtil.yuv_420_888toNv21(image),
                image.getWidth(),
                image.getHeight(),
                shouldCropImage(image) ? image.getCropRect() : null);
    }

    /** Exception for error during transcoding image. */
    public static final class CodecFailedException extends Exception {
        enum FailureType {
            ENCODE_FAILED,
            DECODE_FAILED,
            UNKNOWN
        }

        private FailureType mFailureType;

        CodecFailedException(String message) {
            super(message);
            mFailureType = FailureType.UNKNOWN;
        }

        CodecFailedException(String message, FailureType failureType) {
            super(message);
            mFailureType = failureType;
        }

        public FailureType getFailureType() {
            return mFailureType;
        }
    }
}
