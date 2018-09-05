package one.mixin.android.util.video;

import android.media.*;
import android.os.Build;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.*;
import com.googlecode.mp4parser.util.Path;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import one.mixin.android.crypto.Util;
import timber.log.Timber;

public class MediaController {
    public final static String MIME_TYPE = "video/avc";
    private final static int PROCESSOR_TYPE_OTHER = 0;
    private final static int PROCESSOR_TYPE_QCOM = 1;
    private final static int PROCESSOR_TYPE_INTEL = 2;
    private final static int PROCESSOR_TYPE_MTK = 3;
    private final static int PROCESSOR_TYPE_SEC = 4;
    private final static int PROCESSOR_TYPE_TI = 5;

    private void didWriteData(final File file, final boolean last, final boolean
            error) {

    }

    private int selectTrack(MediaExtractor extractor, boolean audio) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (audio) {
                if (mime.startsWith("audio/")) {
                    return i;
                }
            } else {
                if (mime.startsWith("video/")) {
                    return i;
                }
            }
        }
        return -5;
    }

    private final Object videoConvertSync = new Object();
    private boolean cancelCurrentVideoConversion = false;

    private void checkConversionCanceled() throws Exception {
        boolean cancelConversion;
        synchronized (videoConvertSync) {
            cancelConversion = cancelCurrentVideoConversion;
        }
        if (cancelConversion) {
            throw new RuntimeException("canceled conversion");
        }
    }

    private long readAndWriteTracks(MediaExtractor extractor, MP4Builder mediaMuxer, MediaCodec.BufferInfo info, long start, long end, File file, boolean needAudio) throws Exception {
        int videoTrackIndex = findTrack(extractor, false);
        int audioTrackIndex = needAudio ? findTrack(extractor, true) : -1;
        int muxerVideoTrackIndex = -1;
        int muxerAudioTrackIndex = -1;
        boolean inputDone = false;
        int maxBufferSize = 0;
        if (videoTrackIndex >= 0) {
            extractor.selectTrack(videoTrackIndex);
            MediaFormat trackFormat = extractor.getTrackFormat(videoTrackIndex);
            muxerVideoTrackIndex = mediaMuxer.addTrack(trackFormat, false);
            maxBufferSize = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            if (start > 0) {
                extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            } else {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            }
        }
        if (audioTrackIndex >= 0) {
            extractor.selectTrack(audioTrackIndex);
            MediaFormat trackFormat = extractor.getTrackFormat(audioTrackIndex);
            muxerAudioTrackIndex = mediaMuxer.addTrack(trackFormat, true);
            maxBufferSize = Math.max(trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE), maxBufferSize);
            if (start > 0) {
                extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            } else {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            }
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
        if (audioTrackIndex >= 0 || videoTrackIndex >= 0) {
            long startTime = -1;
            checkConversionCanceled();
            while (!inputDone) {
                checkConversionCanceled();
                boolean eof = false;
                int muxerTrackIndex;
                info.size = extractor.readSampleData(buffer, 0);
                int index = extractor.getSampleTrackIndex();
                if (index == videoTrackIndex) {
                    muxerTrackIndex = muxerVideoTrackIndex;
                } else if (index == audioTrackIndex) {
                    muxerTrackIndex = muxerAudioTrackIndex;
                } else {
                    muxerTrackIndex = -1;
                }
                if (muxerTrackIndex != -1) {
                    if (index != audioTrackIndex) {
                        byte[] array = buffer.array();
                        if (array != null) {
                            int offset = buffer.arrayOffset();
                            int len = offset + buffer.limit();
                            int writeStart = -1;
                            for (int a = offset; a <= len - 4; a++) {
                                if (array[a] == 0 && array[a + 1] == 0 && array[a + 2] == 0 && array[a + 3] == 1 || a == len - 4) {
                                    if (writeStart != -1) {
                                        int l = a - writeStart - (a != len - 4 ? 4 : 0);
                                        array[writeStart] = (byte) (l >> 24);
                                        array[writeStart + 1] = (byte) (l >> 16);
                                        array[writeStart + 2] = (byte) (l >> 8);
                                        array[writeStart + 3] = (byte) l;
                                        writeStart = a;
                                    } else {
                                        writeStart = a;
                                    }
                                }
                            }
                        }
                    }
                    if (info.size >= 0) {
                        info.presentationTimeUs = extractor.getSampleTime();
                    } else {
                        info.size = 0;
                        eof = true;
                    }

                    if (info.size > 0 && !eof) {
                        if (index == videoTrackIndex && start > 0 && startTime == -1) {
                            startTime = info.presentationTimeUs;
                        }
                        if (end < 0 || info.presentationTimeUs < end) {
                            info.offset = 0;
                            info.flags = extractor.getSampleFlags();
                            if (mediaMuxer.writeSampleData(muxerTrackIndex, buffer, info, false)) {
                                didWriteData(file, false, false);
                            }
                        } else {
                            eof = true;
                        }
                    }
                    if (!eof) {
                        extractor.advance();
                    }
                } else if (index == -1) {
                    eof = true;
                } else {
                    extractor.advance();
                }
                if (eof) {
                    inputDone = true;
                }
            }
            if (videoTrackIndex >= 0) {
                extractor.unselectTrack(videoTrackIndex);
            }
            if (audioTrackIndex >= 0) {
                extractor.unselectTrack(audioTrackIndex);
            }
            return startTime;
        }
        return -1;
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    public static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        int lastColorFormat = 0;
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                lastColorFormat = colorFormat;
                if (!(codecInfo.getName().equals("OMX.SEC.AVC.Encoder") && colorFormat == 19)) {
                    return colorFormat;
                }
            }
        }
        return lastColorFormat;
    }


    public static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo lastCodecInfo = null;
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    lastCodecInfo = codecInfo;
                    if (!lastCodecInfo.getName().equals("OMX.SEC.avc.enc")) {
                        return lastCodecInfo;
                    } else if (lastCodecInfo.getName().equals("OMX.SEC.AVC.Encoder")) {
                        return lastCodecInfo;
                    }
                }
            }
        }
        return lastCodecInfo;
    }

    public static int getBitrate(String videoPath, float scale) {
        TrackHeaderBox trackHeaderBox = null;
        float videoDuration;
        int originalBitrate;
        try {
            IsoFile isoFile = new IsoFile(videoPath);
            List<Box> boxes = Path.getPaths(isoFile, "/moov/trak/");

            Box boxTest = Path.getPath(isoFile, "/moov/trak/mdia/minf/stbl/stsd/mp4a/");
            if (boxTest == null) {
                Timber.d("video hasn't mp4a atom");
            }

            boxTest = Path.getPath(isoFile, "/moov/trak/mdia/minf/stbl/stsd/avc1/");
            if (boxTest == null) {
                Timber.d("video hasn't avc1 atom");
            }

            for (int b = 0; b < boxes.size(); b++) {
                Box box = boxes.get(b);
                TrackBox trackBox = (TrackBox) box;
                long sampleSizes = 0;
                long trackBitrate = 0;
                try {
                    MediaBox mediaBox = trackBox.getMediaBox();
                    MediaHeaderBox mediaHeaderBox = mediaBox.getMediaHeaderBox();
                    SampleSizeBox sampleSizeBox = mediaBox.getMediaInformationBox().getSampleTableBox().getSampleSizeBox();
                    long[] sizes = sampleSizeBox.getSampleSizes();
                    for (int a = 0; a < sizes.length; a++) {
                        sampleSizes += sizes[a];
                    }
                    videoDuration = (float) mediaHeaderBox.getDuration() / (float) mediaHeaderBox.getTimescale();
                    trackBitrate = (int) (sampleSizes * 8 / videoDuration);
                } catch (Exception e) {
                    Timber.e(e);
                }
                TrackHeaderBox headerBox = trackBox.getTrackHeaderBox();
                if (headerBox.getWidth() != 0 && headerBox.getHeight() != 0) {
                    if (trackHeaderBox == null || trackHeaderBox.getWidth() < headerBox.getWidth() || trackHeaderBox.getHeight() < headerBox.getHeight()) {
                        trackHeaderBox = headerBox;
                        originalBitrate = (int) (trackBitrate / 100000 * 100000);
                        return Math.min(1100000, (int) (originalBitrate / scale));
                    }
                }
            }

        } catch (Exception e) {
        }
        return 0;
    }

    private int findTrack(MediaExtractor extractor, boolean audio) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (audio) {
                if (mime.startsWith("audio/")) {
                    return i;
                }
            } else {
                if (mime.startsWith("video/")) {
                    return i;
                }
            }
        }
        return -5;
    }

    public boolean convertVideo(String videoPath, int bitrate, int resultWidth, int resultHeight, int
            originalWidth, int originalHeight, File cacheFile, boolean needChange) throws IOException {
        if (!needChange) {
            Util.copy(new FileInputStream(videoPath), new FileOutputStream(cacheFile));
            return true;
        }
        long startTime = -1;
        long endTime = -1;
        int rotateRender = 0;

        boolean error = false;

        long time = System.currentTimeMillis();

        if (resultWidth != 0 && resultHeight != 0) {
            MP4Builder mediaMuxer = null;
            MediaExtractor extractor = null;

            try {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                Mp4Movie movie = new Mp4Movie();
                movie.setCacheFile(cacheFile);
                movie.setRotation(0);
                movie.setSize(resultWidth, resultHeight);
                mediaMuxer = new MP4Builder().createMovie(movie);
                extractor = new MediaExtractor();
                extractor.setDataSource(videoPath);

                checkConversionCanceled();

                if (resultWidth != originalWidth || resultHeight != originalHeight || rotateRender != 0) {
                    int videoIndex = findTrack(extractor, false);
                    int audioIndex = bitrate != -1 ? findTrack(extractor, true) : -1;
                    if (videoIndex >= 0) {
                        MediaCodec decoder = null;
                        MediaCodec encoder = null;
                        InputSurface inputSurface = null;
                        OutputSurface outputSurface = null;

                        try {
                            long videoTime = -1;
                            boolean outputDone = false;
                            boolean inputDone = false;
                            boolean decoderDone = false;
                            int swapUV = 0;
                            int videoTrackIndex = -5;
                            int audioTrackIndex = -5;

                            int colorFormat;
                            int processorType = PROCESSOR_TYPE_OTHER;
                            String manufacturer = Build.MANUFACTURER.toLowerCase();
                            if (Build.VERSION.SDK_INT < 18) {
                                MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
                                colorFormat = selectColorFormat(codecInfo, MIME_TYPE);
                                if (colorFormat == 0) {
                                    throw new RuntimeException("no supported color format");
                                }
                                String codecName = codecInfo.getName();
                                if (codecName.contains("OMX.qcom.")) {
                                    processorType = PROCESSOR_TYPE_QCOM;
                                    if (Build.VERSION.SDK_INT == 16) {
                                        if (manufacturer.equals("lge") || manufacturer.equals("nokia")) {
                                            swapUV = 1;
                                        }
                                    }
                                } else if (codecName.contains("OMX.Intel.")) {
                                    processorType = PROCESSOR_TYPE_INTEL;
                                } else if (codecName.equals("OMX.MTK.VIDEO.ENCODER.AVC")) {
                                    processorType = PROCESSOR_TYPE_MTK;
                                } else if (codecName.equals("OMX.SEC.AVC.Encoder")) {
                                    processorType = PROCESSOR_TYPE_SEC;
                                    swapUV = 1;
                                } else if (codecName.equals("OMX.TI.DUCATI1.VIDEO.H264E")) {
                                    processorType = PROCESSOR_TYPE_TI;
                                }
                                Timber.d("codec = " + codecInfo.getName() + " manufacturer = " + manufacturer + "device = " + Build.MODEL);
                            } else {
                                colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
                            }
                            Timber.d("colorFormat = " + colorFormat);

                            int resultHeightAligned = resultHeight;
                            int padding = 0;
                            int bufferSize = resultWidth * resultHeight * 3 / 2;
                            if (processorType == PROCESSOR_TYPE_OTHER) {
                                if (resultHeight % 16 != 0) {
                                    resultHeightAligned += (16 - (resultHeight % 16));
                                    padding = resultWidth * (resultHeightAligned - resultHeight);
                                    bufferSize += padding * 5 / 4;
                                }
                            } else if (processorType == PROCESSOR_TYPE_QCOM) {
                                if (!manufacturer.toLowerCase().equals("lge")) {
                                    int uvoffset = (resultWidth * resultHeight + 2047) & ~2047;
                                    padding = uvoffset - (resultWidth * resultHeight);
                                    bufferSize += padding;
                                }
                            } else if (processorType == PROCESSOR_TYPE_TI) {
                                //resultHeightAligned = 368;
                                //bufferSize = resultWidth * resultHeightAligned * 3 / 2;
                                //resultHeightAligned += (16 - (resultHeight % 16));
                                //padding = resultWidth * (resultHeightAligned - resultHeight);
                                //bufferSize += padding * 5 / 4;
                            } else if (processorType == PROCESSOR_TYPE_MTK) {
                                if (manufacturer.equals("baidu")) {
                                    resultHeightAligned += (16 - (resultHeight % 16));
                                    padding = resultWidth * (resultHeightAligned - resultHeight);
                                    bufferSize += padding * 5 / 4;
                                }
                            }

                            extractor.selectTrack(videoIndex);
                            MediaFormat videoFormat = extractor.getTrackFormat(videoIndex);
                            ByteBuffer audioBuffer = null;
                            if (audioIndex >= 0) {
                                extractor.selectTrack(audioIndex);
                                MediaFormat audioFormat = extractor.getTrackFormat(audioIndex);
                                int maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                                audioBuffer = ByteBuffer.allocateDirect(maxBufferSize);
                                audioTrackIndex = mediaMuxer.addTrack(audioFormat, true);
                            }

                            if (startTime > 0) {
                                extractor.seekTo(startTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                            } else {
                                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                            }

                            MediaFormat outputFormat = MediaFormat.createVideoFormat(MIME_TYPE, resultWidth, resultHeight);
                            outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
                            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate > 0 ? bitrate : 921600);
                            outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
                            outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
                            /*if (Build.VERSION.SDK_INT >= 21) {
                                outputFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
                                if (Build.VERSION.SDK_INT >= 23) {
                                    outputFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel5);
                                }
                            }*/
                            if (Build.VERSION.SDK_INT < 18) {
                                outputFormat.setInteger("stride", resultWidth + 32);
                                outputFormat.setInteger("slice-height", resultHeight);
                            }

                            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
                            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                            if (Build.VERSION.SDK_INT >= 18) {
                                inputSurface = new InputSurface(encoder.createInputSurface());
                                inputSurface.makeCurrent();
                            }
                            encoder.start();

                            decoder = MediaCodec.createDecoderByType(videoFormat.getString(MediaFormat.KEY_MIME));
                            if (Build.VERSION.SDK_INT >= 18) {
                                outputSurface = new OutputSurface();
                            } else {
                                outputSurface = new OutputSurface(resultWidth, resultHeight, rotateRender);
                            }
                            decoder.configure(videoFormat, outputSurface.getSurface(), null, 0);
                            decoder.start();

                            final int TIMEOUT_USEC = 2500;
                            ByteBuffer[] decoderInputBuffers = null;
                            ByteBuffer[] encoderOutputBuffers = null;
                            ByteBuffer[] encoderInputBuffers = null;
                            if (Build.VERSION.SDK_INT < 21) {
                                decoderInputBuffers = decoder.getInputBuffers();
                                encoderOutputBuffers = encoder.getOutputBuffers();
                                if (Build.VERSION.SDK_INT < 18) {
                                    encoderInputBuffers = encoder.getInputBuffers();
                                }
                            }

                            checkConversionCanceled();

                            while (!outputDone) {
                                checkConversionCanceled();
                                if (!inputDone) {
                                    boolean eof = false;
                                    int index = extractor.getSampleTrackIndex();
                                    if (index == videoIndex) {
                                        int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                                        if (inputBufIndex >= 0) {
                                            ByteBuffer inputBuf;
                                            if (Build.VERSION.SDK_INT < 21) {
                                                inputBuf = decoderInputBuffers[inputBufIndex];
                                            } else {
                                                inputBuf = decoder.getInputBuffer(inputBufIndex);
                                            }
                                            int chunkSize = extractor.readSampleData(inputBuf, 0);
                                            if (chunkSize < 0) {
                                                decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                                inputDone = true;
                                            } else {
                                                decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, extractor.getSampleTime(), 0);
                                                extractor.advance();
                                            }
                                        }
                                    } else if (audioIndex != -1 && index == audioIndex) {
                                        info.size = extractor.readSampleData(audioBuffer, 0);
                                        if (Build.VERSION.SDK_INT < 21) {
                                            audioBuffer.position(0);
                                            audioBuffer.limit(info.size);
                                        }
                                        if (info.size >= 0) {
                                            info.presentationTimeUs = extractor.getSampleTime();
                                            extractor.advance();
                                        } else {
                                            info.size = 0;
                                            inputDone = true;
                                        }
                                        if (info.size > 0 && (endTime < 0 || info.presentationTimeUs < endTime)) {
                                            info.offset = 0;
                                            info.flags = extractor.getSampleFlags();
                                            if (mediaMuxer.writeSampleData(audioTrackIndex, audioBuffer, info, false)) {
                                                didWriteData(cacheFile, false, false);
                                            }
                                        }
                                    } else if (index == -1) {
                                        eof = true;
                                    }
                                    if (eof) {
                                        int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                                        if (inputBufIndex >= 0) {
                                            decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                            inputDone = true;
                                        }
                                    }
                                }

                                boolean decoderOutputAvailable = !decoderDone;
                                boolean encoderOutputAvailable = true;
                                while (decoderOutputAvailable || encoderOutputAvailable) {
                                    checkConversionCanceled();
                                    int encoderStatus = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                        encoderOutputAvailable = false;
                                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                        if (Build.VERSION.SDK_INT < 21) {
                                            encoderOutputBuffers = encoder.getOutputBuffers();
                                        }
                                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                        MediaFormat newFormat = encoder.getOutputFormat();
                                        if (videoTrackIndex == -5) {
                                            videoTrackIndex = mediaMuxer.addTrack(newFormat, false);
                                        }
                                    } else if (encoderStatus < 0) {
                                        throw new RuntimeException("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                                    } else {
                                        ByteBuffer encodedData;
                                        if (Build.VERSION.SDK_INT < 21) {
                                            encodedData = encoderOutputBuffers[encoderStatus];
                                        } else {
                                            encodedData = encoder.getOutputBuffer(encoderStatus);
                                        }
                                        if (encodedData == null) {
                                            throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                                        }
                                        if (info.size > 1) {
                                            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                                if (mediaMuxer.writeSampleData(videoTrackIndex, encodedData, info, true)) {
                                                    didWriteData(cacheFile, false, false);
                                                }
                                            } else if (videoTrackIndex == -5) {
                                                byte[] csd = new byte[info.size];
                                                encodedData.limit(info.offset + info.size);
                                                encodedData.position(info.offset);
                                                encodedData.get(csd);
                                                ByteBuffer sps = null;
                                                ByteBuffer pps = null;
                                                for (int a = info.size - 1; a >= 0; a--) {
                                                    if (a > 3) {
                                                        if (csd[a] == 1 && csd[a - 1] == 0 && csd[a - 2] == 0 && csd[a - 3] == 0) {
                                                            sps = ByteBuffer.allocate(a - 3);
                                                            pps = ByteBuffer.allocate(info.size - (a - 3));
                                                            sps.put(csd, 0, a - 3).position(0);
                                                            pps.put(csd, a - 3, info.size - (a - 3)).position(0);
                                                            break;
                                                        }
                                                    } else {
                                                        break;
                                                    }
                                                }

                                                MediaFormat newFormat = MediaFormat.createVideoFormat(MIME_TYPE, resultWidth, resultHeight);
                                                if (sps != null && pps != null) {
                                                    newFormat.setByteBuffer("csd-0", sps);
                                                    newFormat.setByteBuffer("csd-1", pps);
                                                }
                                                videoTrackIndex = mediaMuxer.addTrack(newFormat, false);
                                            }
                                        }
                                        outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                                        encoder.releaseOutputBuffer(encoderStatus, false);
                                    }
                                    if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                                        continue;
                                    }

                                    if (!decoderDone) {
                                        int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                                        if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                            decoderOutputAvailable = false;
                                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                            MediaFormat newFormat = decoder.getOutputFormat();
                                            Timber.d("newFormat = " + newFormat);
                                        } else if (decoderStatus < 0) {
                                            throw new RuntimeException("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                                        } else {
                                            boolean doRender;
                                            if (Build.VERSION.SDK_INT >= 18) {
                                                doRender = info.size != 0;
                                            } else {
                                                doRender = info.size != 0 || info.presentationTimeUs != 0;
                                            }
                                            if (endTime > 0 && info.presentationTimeUs >= endTime) {
                                                inputDone = true;
                                                decoderDone = true;
                                                doRender = false;
                                                info.flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                                            }
                                            if (startTime > 0 && videoTime == -1) {
                                                if (info.presentationTimeUs < startTime) {
                                                    doRender = false;
                                                    Timber.d("drop frame startTime = " + startTime + " present time = " + info.presentationTimeUs);
                                                } else {
                                                    videoTime = info.presentationTimeUs;
                                                }
                                            }
                                            decoder.releaseOutputBuffer(decoderStatus, doRender);
                                            if (doRender) {
                                                boolean errorWait = false;
                                                try {
                                                    outputSurface.awaitNewImage();
                                                } catch (Exception e) {
                                                    errorWait = true;
                                                    Timber.e(e);
                                                }
                                                if (!errorWait) {
                                                    if (Build.VERSION.SDK_INT >= 18) {
                                                        outputSurface.drawImage(false);
                                                        inputSurface.setPresentationTime(info.presentationTimeUs * 1000);
                                                        inputSurface.swapBuffers();
                                                    } else {
                                                        int inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                                                        if (inputBufIndex >= 0) {
                                                            outputSurface.drawImage(true);
                                                            ByteBuffer rgbBuf = outputSurface.getFrame();
                                                            ByteBuffer yuvBuf = encoderInputBuffers[inputBufIndex];
                                                            yuvBuf.clear();
                                                            encoder.queueInputBuffer(inputBufIndex, 0, bufferSize, info.presentationTimeUs, 0);
                                                        } else {
                                                            Timber.d("input buffer not available");
                                                        }
                                                    }
                                                }
                                            }
                                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                                decoderOutputAvailable = false;
                                                Timber.d("decoder stream end");
                                                if (Build.VERSION.SDK_INT >= 18) {
                                                    encoder.signalEndOfInputStream();
                                                } else {
                                                    int inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                                                    if (inputBufIndex >= 0) {
                                                        encoder.queueInputBuffer(inputBufIndex, 0, 1, info.presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Timber.e(e);
                            error = true;
                        }

                        extractor.unselectTrack(videoIndex);

                        if (outputSurface != null) {
                            outputSurface.release();
                        }
                        if (inputSurface != null) {
                            inputSurface.release();
                        }
                        if (decoder != null) {
                            decoder.stop();
                            decoder.release();
                        }
                        if (encoder != null) {
                            encoder.stop();
                            encoder.release();
                        }
                        checkConversionCanceled();
                    }
                } else {
                    readAndWriteTracks(extractor, mediaMuxer, info, startTime, endTime, cacheFile, bitrate != -1);
                }
            } catch (Exception e) {
                error = true;
                Timber.e(e);
            } finally {
                if (extractor != null) {
                    extractor.release();
                }
                if (mediaMuxer != null) {
                    try {
                        mediaMuxer.finishMovie();
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }
                Timber.d("time = " + (System.currentTimeMillis() - time));
            }
        } else {
            didWriteData(cacheFile, true, true);
            return false;
        }
        didWriteData(cacheFile, true, error);
        return true;
    }
}
