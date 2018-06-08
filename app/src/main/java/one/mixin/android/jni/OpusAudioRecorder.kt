package one.mixin.android.jni

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.content.systemService
import one.mixin.android.AppExecutors
import one.mixin.android.extension.createAudioTemp
import one.mixin.android.extension.getAudioPath
import one.mixin.android.util.DispatchQueue
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

    private val recordQueue: DispatchQueue by lazy {
        DispatchQueue("recordQueue").apply {
            priority = Thread.MAX_PRIORITY
        }
    }
    private val fileEncodingQueue: DispatchQueue by lazy {
        DispatchQueue("fileEncodingQueue").apply {
            priority = Thread.MAX_PRIORITY
        }
    }

    var callback: Callback? = null

    init {
        recordBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (recordBufferSize <= 0) {
            recordBufferSize = 1280
        }
        for (i in 0 until 5) {
            val buffer = ByteBuffer.allocateDirect(4096)
            buffer.order(ByteOrder.nativeOrder())
            recordBuffers.add(buffer)
        }
        fileBuffer = ByteBuffer.allocateDirect(1920)

        try {
            val phoneStateListener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                    if (state != TelephonyManager.CALL_STATE_IDLE) {
                        stopRecording(false)
                        callback?.onCancel()
                    }
                }
            }
            ctx.systemService<TelephonyManager>()?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (ignore: Exception) {
        }
    }

    private val recordRunnable: Runnable by lazy {
        Runnable {
            audioRecord?.let { audioRecord ->
                val shortArray = ShortArray(recordBufferSize)
                val len = audioRecord.read(shortArray, 0, recordBufferSize)
                if (len > 0) {
                    var sum = 0
                    try {
                        val newSamplesCount = samplesCount + len / 2
                        val currPart = (samplesCount / newSamplesCount.toDouble() * recordSamples.size).toInt()
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
                        for (i in 0 until len) {
                            val peak = shortArray[i]
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

                    fileEncodingQueue.postRunnable(Runnable {
                        writeFrame(shortArray, len)
                        recordTimeCount += len / 16
                    })
                    recordQueue.postRunnable(recordRunnable)
                } else {
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
            fileBuffer.rewind()

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

        recordQueue.postRunnable(recordRunnable)
    }

    fun startRecording() {
        recordQueue.postRunnable(recodeStartRunnable)
    }

    fun stopRecording(send: Boolean) {
        recordQueue.cancelRunnable(recodeStartRunnable)
        recordQueue.postRunnable(Runnable {
            audioRecord?.let { audioRecord ->
                try {
                    sendAfterDone = send
                    audioRecord.stop()
                } catch (e: Exception) {
                    recordingAudioFile?.delete()
                }
                stopRecordingInternal(send)
            }
        })
    }

    private fun stopRecordingInternal(send: Boolean) {
        if (send) {
            fileEncodingQueue.postRunnable(Runnable {
                stopRecord()

                AppExecutors().mainThread().execute {
                    val duration = recordTimeCount
                    val waveForm = getWaveform2(recordSamples, recordSamples.size)
                    if (recordingAudioFile != null) {
                        callback?.sendAudio(recordingAudioFile!!, duration, waveForm)
                    }
                    recordingAudioFile = null
                }
            })
        }

        try {
            audioRecord?.release()
            audioRecord = null
        } catch (ignore: Exception) {
        }
    }

    private external fun startRecord(path: String): Int
    private external fun writeFrame(frame: ShortArray, len: Int): Int
    private external fun stopRecord()
    private external fun getWaveform2(arr: ShortArray, len: Int): ByteArray

    interface Callback {
        fun onCancel()
        fun sendAudio(file: File, duration: Long, waveForm: ByteArray)
    }
}