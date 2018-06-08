#include <jni.h>
#include <stdio.h>
#include <opus.h>
#include <opusenc.h>
#include <stdlib.h>
#include "utils.h"

FILE *fin;
OggOpusEnc *enc;
OggOpusComments *comments;
int error;

static inline void set_bits(uint8_t *bytes, int32_t bitOffset, int32_t value) {
    bytes += bitOffset / 8;
    bitOffset %= 8;
    *((int32_t *) bytes) |= (value << bitOffset);
}

JNIEXPORT int Java_one_mixin_android_jni_OpusAudioRecorder_startRecord(JNIEnv *env, jclass class, jstring path) {
    const char *pathStr = (*env)->GetStringUTFChars(env, path, 0);
    if (!pathStr) {
        LOGE("Error path");
        return 0;
    }
    fin = fopen(pathStr, "rb");
    if (!fin) {
        LOGE("Open file failed");
        return 0;
    }
    comments = ope_comments_create();
    enc = ope_encoder_create_file(pathStr, comments, 16000, 1, 0, &error);
    if (!enc) {
        LOGE("Create OggOpusEnc failed");
        fclose(fin);
        return 0;
    }
    return 1;
}

JNIEXPORT int
Java_one_mixin_android_jni_OpusAudioRecorder_writeFrame(JNIEnv *env, jclass class, jobject frame, jint len) {
    jshort *frameShort = (*env) -> GetShortArrayElements(env, frame, 0);
    ope_encoder_write(enc, frameShort, len);
    return 1;
}

JNIEXPORT void Java_one_mixin_android_jni_OpusAudioRecorder_stopRecord(JNIEnv *env, jclass class) {
    ope_encoder_drain(enc);
    LOGE("ope_encoder_drain");
    ope_encoder_destroy(enc);
    LOGE("ope_encoder_destroy");
    ope_comments_destroy(comments);
    LOGE("ope_comments_destroy");
    fclose(fin);
    LOGE("Close file");
}

JNIEXPORT jbyteArray
Java_one_mixin_android_jni_OpusAudioRecorder_getWaveform2(JNIEnv *env, jclass class, jshortArray array, jint length) {
    jshort *sampleBuffer = (*env)->GetShortArrayElements(env, array, 0);

    jbyteArray result = 0;
    int32_t resultSamples = 100;
    uint16_t *samples = malloc(100 * 2);
    uint64_t sampleIndex = 0;
    uint16_t peakSample = 0;
    int32_t sampleRate = (int32_t) max(1, length / resultSamples);
    int index = 0;

    for (int i = 0; i < length; i++) {
        uint16_t sample = (uint16_t) abs(sampleBuffer[i]);
        if (sample > peakSample) {
            peakSample = sample;
        }
        if (sampleIndex++ % sampleRate == 0) {
            if (index < resultSamples) {
                samples[index++] = peakSample;
            }
            peakSample = 0;
        }
    }

    int64_t sumSamples = 0;
    for (int i = 0; i < resultSamples; i++) {
        sumSamples += samples[i];
    }
    uint16_t peak = (uint16_t) (sumSamples * 1.8f / resultSamples);
    if (peak < 2500) {
        peak = 2500;
    }

    for (int i = 0; i < resultSamples; i++) {
        uint16_t sample = (uint16_t) ((int64_t) samples[i]);
        if (sample > peak) {
            samples[i] = peak;
        }
    }

    (*env)->ReleaseShortArrayElements(env, array, sampleBuffer, 0);

    int bitstreamLength = (resultSamples * 5) / 8 + (((resultSamples * 5) % 8) == 0 ? 0 : 1);
    result = (*env)->NewByteArray(env, bitstreamLength);
    jbyte *bytes = (*env)->GetByteArrayElements(env, result, NULL);

    for (int i = 0; i < resultSamples; i++) {
        int32_t value = min(31, abs((int32_t) samples[i]) * 31 / peak);
        set_bits(bytes, i * 5, value & 31);
    }

    (*env)->ReleaseByteArrayElements(env, result, bytes, JNI_COMMIT);
    free(samples);

    return result;
}