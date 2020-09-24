package one.mixin.android.media

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.extension.createAudioTemp
import one.mixin.android.extension.getLegacyAudioPath
import one.mixin.android.extension.tapVibrate
import one.mixin.android.util.DispatchQueue
import java.io.File
import java.util.UUID

class OpusAudioRecorder private constructor(private val ctx: Context) {
    companion object {
        init {
            System.loadLibrary("mixin")
        }

        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE_FACTOR = 2

        const val STATE_NOT_INIT = 0
        const val STATE_IDLE = 1
        const val STATE_RECORDING = 2

        private const val MAX_RECORD_DURATION = 60000

        var state: Int = STATE_NOT_INIT

        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: OpusAudioRecorder? = null

        @Synchronized
        fun get(conversationId: String): OpusAudioRecorder {
            if (INSTANCE == null) {
                INSTANCE = OpusAudioRecorder(MixinApplication.appContext)
                state = STATE_IDLE
            }
            INSTANCE?.initConversation(conversationId)
            return INSTANCE as OpusAudioRecorder
        }
    }

    private var audioRecord: AudioRecord? = null
    private var recordBufferSize: Int
    private var recordingAudioFile: File? = null
    private var messageId: String? = null
    private var conversationId: String? = null
    private val recordSamples = ShortArray(1024)
    private var samplesCount = 0L
    private var recordTimeCount = 0L
    private var sendAfterDone = false
    private var callStop = false

    private fun initConversation(conversationId: String) {
        if (this.conversationId != conversationId) {
            this.conversationId = conversationId
        }
    }

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

    private var callback: Callback? = null

    init {
        recordBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

        try {
            val phoneStateListener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                    if (state != TelephonyManager.CALL_STATE_IDLE) {
                        stopRecording(AudioEndStatus.CANCEL)
                        callback?.onCancel()
                    }
                }
            }
            ctx.getSystemService<TelephonyManager>()?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (ignore: Exception) {
        }
    }

    private val recordRunnable: Runnable by lazy {
        Runnable recordRunnable@{
            audioRecord?.let { audioRecord ->
                val shortArray = ShortArray(recordBufferSize)
                val len = audioRecord.read(shortArray, 0, shortArray.size)
                if (len > 0 && !callStop) {
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

                    fileEncodingQueue.postRunnable(
                        Runnable encodingRunnable@{
                            if (callStop) return@encodingRunnable

                            writeFrame(shortArray, len)
                            recordTimeCount += len / 16

                            if (recordTimeCount >= MAX_RECORD_DURATION) {
                                stopRecording(AudioEndStatus.SEND, false)
                            }
                        }
                    )
                    recordQueue.postRunnable(recordRunnable)
                } else {
                    stopRecordingInternal(
                        if (sendAfterDone) {
                            AudioEndStatus.SEND
                        } else {
                            AudioEndStatus.CANCEL
                        }
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private val recodeStartRunnable = Runnable {
        if (audioRecord != null) {
            return@Runnable
        }

        messageId = UUID.randomUUID().toString()
        if (conversationId == null) throw IllegalArgumentException("Conversation id is NULL!!!")
        recordingAudioFile = ctx.getLegacyAudioPath().createAudioTemp(conversationId!!, messageId!!, "ogg")

        try {
            if (startRecord(recordingAudioFile!!.absolutePath) != 0) {
                return@Runnable
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recordBufferSize * BUFFER_SIZE_FACTOR
            )

            if (audioRecord == null || audioRecord!!.state != AudioRecord.STATE_INITIALIZED) {
                return@Runnable
            }
            callStop = false
            samplesCount = 0
            recordTimeCount = 0
            audioRecord?.startRecording()

            if (audioRecord != null && audioRecord!!.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.release()
                audioRecord = null
                return@Runnable
            }
            ctx.tapVibrate()
            state = STATE_RECORDING
        } catch (e: Exception) {
            recordingAudioFile?.delete()
            try {
                stopRecord()
                state = STATE_IDLE
                audioRecord?.release()
                audioRecord = null
            } catch (ignore: Exception) {
            }
            return@Runnable
        }

        recordQueue.postRunnable(recordRunnable)
    }

    fun startRecording(callback: Callback) {
        this.callback = callback
        recordQueue.postRunnable(recodeStartRunnable)
    }

    fun stopRecording(endStatus: AudioEndStatus, vibrate: Boolean = true) {
        recordQueue.cancelRunnable(recodeStartRunnable)
        if (vibrate) {
            ctx.tapVibrate()
        }
        recordQueue.postRunnable(
            {
                audioRecord?.let { audioRecord ->
                    try {
                        sendAfterDone = endStatus == AudioEndStatus.SEND
                        if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            audioRecord.stop()
                        }
                    } catch (e: Exception) {
                        recordingAudioFile?.delete()
                    }
                    stopRecordingInternal(endStatus)
                }
            }
        )
    }

    fun stop() {
        callback = null
        stopRecording(AudioEndStatus.CANCEL, false)
    }

    private fun stopRecordingInternal(endStatus: AudioEndStatus) {
        callStop = true
        // if not send no need to stopping record after all encoding runnable run completed.
        if (endStatus != AudioEndStatus.CANCEL) {
            fileEncodingQueue.postRunnable(
                {
                    stopRecord()
                    val duration = recordTimeCount
                    val waveForm = getWaveform2(recordSamples, recordSamples.size)
                    MixinApplication.appScope.launch(Dispatchers.Main) {
                        if (recordingAudioFile != null) {
                            if (endStatus == AudioEndStatus.SEND) {
                                callback?.sendAudio(messageId!!, recordingAudioFile!!, duration, waveForm)
                            } else if (endStatus == AudioEndStatus.PREVIEW) {
                                callback?.previewAudio(messageId!!, recordingAudioFile!!, duration, waveForm)
                            }
                        }
                        callback = null
                        recordingAudioFile = null
                    }
                }
            )
        }
        state = STATE_IDLE
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
        fun sendAudio(messageId: String, file: File, duration: Long, waveForm: ByteArray)
        fun previewAudio(messageId: String, file: File, duration: Long, waveForm: ByteArray)
    }
}
