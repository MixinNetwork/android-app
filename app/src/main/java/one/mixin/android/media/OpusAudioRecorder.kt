package one.mixin.android.media

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
import one.mixin.android.extension.vibrate
import one.mixin.android.util.DispatchQueue
import java.io.File

class OpusAudioRecorder(private val ctx: Context) {
    companion object {
        init {
            System.loadLibrary("mixin")
        }
        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE_FACTOR = 2
    }

    private var audioRecord: AudioRecord? = null
    private var recordBufferSize: Int
    private var recordingAudioFile: File? = null
    private val recordSamples = ShortArray(1024)
    private var samplesCount = 0L
    private var recordTimeCount = 0L
    private var sendAfterDone = false
    var statusSuccess = false

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
        recordBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

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
                val len = audioRecord.read(shortArray, 0, shortArray.size)
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
            if (startRecord(recordingAudioFile!!.absolutePath) != 0) {
                return@Runnable
            }

            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, recordBufferSize * BUFFER_SIZE_FACTOR)

            if (audioRecord == null || audioRecord!!.state != AudioRecord.STATE_INITIALIZED) {
                return@Runnable
            }
            samplesCount = 0
            recordTimeCount = 0
            audioRecord?.startRecording()

            if (audioRecord != null && audioRecord!!.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.release()
                audioRecord = null
                return@Runnable
            }
            ctx.vibrate(longArrayOf(0, 10))
            statusSuccess = true
        } catch (e: Exception) {
            recordingAudioFile?.delete()
            try {
                stopRecord()
                statusSuccess = false
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

    fun stopRecording(send: Boolean, vibrate: Boolean = true) {
        recordQueue.cancelRunnable(recodeStartRunnable)
        if (vibrate) ctx.vibrate(longArrayOf(0, 10))
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

        statusSuccess = false
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