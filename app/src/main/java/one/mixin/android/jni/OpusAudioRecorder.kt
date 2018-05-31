package one.mixin.android.jni

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import one.mixin.android.AppExecutors
import one.mixin.android.extension.createAudioTemp
import one.mixin.android.extension.getAudioPath
import one.mixin.android.util.video.MixinPlayer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OpusAudioRecorder(ctx: Context) {
    companion object {
        init {
            System.loadLibrary("mixin")
        }
    }

    private var audioRecord: AudioRecord? = null
    private var recordBufferSize: Int
    private val recordBuffers = arrayListOf<ByteBuffer>()
    private var fileBuffer: ByteBuffer
    private var recordingAudioFile: File? = null
    private val recordSamples = ShortArray(1024)
    private var samplesCount = 0L
    private var recordTimeCount = 0L
    private var sendAfterDone = false

    private val mixinPlayer = MixinPlayer()

    var callback: Callback? = null

    init {
        recordBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (recordBufferSize <= 0) {
            recordBufferSize = 1280
        }
        for (i in 0..5) {
            val buffer = ByteBuffer.allocateDirect(4096)
            buffer.order(ByteOrder.nativeOrder())
            recordBuffers.add(buffer)
        }
        fileBuffer = ByteBuffer.allocateDirect(1920)
    }

    private val recordRunnable: Runnable by lazy {
        Runnable {
            audioRecord?.let { audioRecord ->
                val buffer = if (recordBuffers.isNotEmpty()) {
                    val b = recordBuffers[0]
                    recordBuffers.removeAt(0)
                    b
                } else {
                    val b = ByteBuffer.allocateDirect(recordBufferSize)
                    b.order(ByteOrder.nativeOrder())
                }
                buffer.rewind()

                val len = audioRecord.read(buffer, buffer.capacity())
                if (len > 0) {
                    buffer.limit(len)
                    var sum = 0
                    try {
                        val newSamplesCount = samplesCount + len / 2
                        val currPart = (samplesCount / newSamplesCount * recordSamples.size).toInt()
                        val newPart = recordSamples.size - currPart
                        var sampleStep: Float
                        if (currPart != 0) {
                            sampleStep = recordSamples.size / currPart.toFloat()
                            var currNum = 0f
                            for (i in 0 until currPart) {
                                recordSamples[i] = recordSamples[currNum.toInt()]
                                currNum += sampleStep
                            }
                        }
                        var currNum = currPart
                        var nextNum = 0f
                        sampleStep = len / 2f / newPart
                        for (i in 0 until len / 2) {
                            val peak = buffer.short
                            if (peak > 2500) {
                                sum += peak * peak
                            }
                            if (i == nextNum.toInt() && currNum < recordSamples.size) {
                                recordSamples[currNum] = peak
                                nextNum += sampleStep
                                currNum++
                            }
                        }
                        samplesCount = newSamplesCount
                    } catch (e: Exception) {
                    }

                    buffer.position(0)
                    val flush = len != buffer.capacity()
                    AppExecutors().diskIO().execute {
                        var oldLimit = -1
                        while (buffer.hasRemaining()) {
                            if (buffer.remaining() > fileBuffer.remaining()) {
                                oldLimit = buffer.limit()
                                buffer.limit(fileBuffer.remaining() + buffer.position())
                            }
                            fileBuffer.put(buffer)
                            if (fileBuffer.position() == fileBuffer.limit() || flush) {
                                if (writeFrame(fileBuffer, if (!flush) fileBuffer.limit() else buffer.position()) != 0) {
                                    fileBuffer.rewind()
                                    recordTimeCount += fileBuffer.limit() / 2 / 16
                                }
                            }
                            if (oldLimit != -1) {
                                buffer.limit(oldLimit)
                            }
                            recordBuffers.add(buffer)
                        }
                    }
                    AppExecutors().diskIO().execute(recordRunnable)
                } else {
                    recordBuffers.add(buffer)
                    stopRecordingInternal(sendAfterDone)
                }
            }
        }
    }

    private val recodeStartRunnable = Runnable {
        if (audioRecord != null) {
            return@Runnable
        }

        recordingAudioFile = ctx.getAudioPath().createAudioTemp("ogg")

        try {
            if (startRecord(recordingAudioFile!!.absolutePath) == 0) {
                return@Runnable
            }

            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, recordBufferSize * 10)
            samplesCount = 0
            recordTimeCount = 0

            audioRecord?.startRecording()
        } catch (e: Exception) {
            stopRecord()
            recordingAudioFile?.delete()
            try {
                audioRecord?.release()
                audioRecord = null
            } catch (ignore: Exception) {
            }
            return@Runnable
        }

        AppExecutors().diskIO().execute(recordRunnable)
    }

    fun startRecording() {
        AppExecutors().diskIO().execute(recodeStartRunnable)
    }

    fun stopRecording(send: Boolean) {
        // remove startRecordingRunnable
        AppExecutors().diskIO().execute {
            audioRecord?.let { audioRecord ->
                try {
                    sendAfterDone = send
                    audioRecord.stop()
                } catch (e: Exception) {
                    recordingAudioFile?.delete()
                }
                stopRecordingInternal(send)
            }
        }
    }

    fun playAudio() {
        recordingAudioFile?.let {
            mixinPlayer.loadVideo(it.absolutePath)
            mixinPlayer.start()
        }
    }

    private fun stopRecordingInternal(send: Boolean) {
        if (send) {
            AppExecutors().mainThread().execute {
                val duration = recordTimeCount / 1000
                val waveForm = getWaveform2(recordSamples, recordSamples.size)
                if (recordingAudioFile != null) {
                    callback?.sendAudio(recordingAudioFile!!, duration, waveForm)
                }
                recordingAudioFile = null
            }
        }

        try {
            audioRecord?.release()
            audioRecord = null
        } catch (ignore: Exception) {
        }
    }

    private external fun startRecord(path: String): Int
    private external fun writeFrame(frame: ByteBuffer, len: Int): Int
    private external fun stopRecord()
    private external fun getWaveform2(arr: ShortArray, len: Int): ByteArray

    interface Callback {
        fun sendAudio(file: File, duration: Long, waveForm: ByteArray)
    }
}