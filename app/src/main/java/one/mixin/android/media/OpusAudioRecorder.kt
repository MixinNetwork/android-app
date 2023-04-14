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
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.createAudioPreviewTemp
import one.mixin.android.extension.createAudioTemp
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.getAudioPreviewTemp
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.util.DispatchQueue
import one.mixin.android.util.GsonHelper
import timber.log.Timber
import java.io.File
import java.util.UUID

class OpusAudioRecorder private constructor(private val ctx: Context) {
    companion object {
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

        fun getAudioPreview(context: Context, conversationId: String): AudioPreview? {
            val audioPreviewFile = context.getAudioPath().getAudioPreviewTemp(conversationId)
            if (!audioPreviewFile.exists()) return null

            audioPreviewFile.bufferedReader().use { br ->
                val data = br.readText()
                return try {
                    GsonHelper.customGson.fromJson(data, AudioPreview::class.java)
                } catch (e: Exception) {
                    Timber.i("deserialize AudioPreview fails ${e.stackTraceToString()}")
                    null
                }
            }
        }

        fun deletePreviewAudio(context: Context, conversationId: String) {
            val audioPreviewFile = context.getAudioPath().getAudioPreviewTemp(conversationId)
            if (audioPreviewFile.exists()) {
                try {
                    audioPreviewFile.delete()
                } catch (e: Exception) {
                    Timber.i("delete AudioPreview fails ${e.stackTraceToString()}")
                }
            }
        }
    }

    data class AudioPreview(
        val messageId: String,
        val path: String,
        val duration: Long,
        val waveForm: ByteArray,
    )

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
                        },
                    )
                    recordQueue.postRunnable(recordRunnable)
                } else {
                    stopRecordingInternal(
                        if (sendAfterDone) {
                            AudioEndStatus.SEND
                        } else {
                            AudioEndStatus.CANCEL
                        },
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
        recordingAudioFile = ctx.getAudioPath().createAudioTemp(conversationId!!, messageId!!, "ogg")

        try {
            if (startRecord(recordingAudioFile!!.absolutePath) != 0) {
                return@Runnable
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recordBufferSize * BUFFER_SIZE_FACTOR,
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
            ctx.heavyClickVibrate()
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

    fun stopRecording(endStatus: AudioEndStatus, vibrate: Boolean = true, notify: Boolean = true) {
        recordQueue.cancelRunnable(recodeStartRunnable)
        if (vibrate) {
            ctx.clickVibrate()
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
                    stopRecordingInternal(endStatus, notify)
                }
            },
        )
    }

    fun stop() {
        callback = null
        stopRecording(AudioEndStatus.CANCEL, false)
    }

    private fun stopRecordingInternal(endStatus: AudioEndStatus, notify: Boolean = true) {
        callStop = true
        // if not send no need to stopping record after all encoding runnable run completed.
        if (endStatus != AudioEndStatus.CANCEL) {
            fileEncodingQueue.postRunnable(
                {
                    stopRecord()
                    val duration = recordTimeCount
                    val waveForm = getWaveform2(recordSamples, recordSamples.size)
                    val conversationId = this.conversationId ?: return@postRunnable
                    val messageId = this.messageId ?: return@postRunnable
                    val recordingAudioFile = this.recordingAudioFile ?: return@postRunnable

                    if (duration >= 500) {
                        val audioPreview = AudioPreview(
                            messageId,
                            recordingAudioFile.absolutePath,
                            duration,
                            waveForm,
                        )
                        val previewFile = ctx.getAudioPath().createAudioPreviewTemp(conversationId)
                        previewFile.bufferedWriter().use { fos ->
                            val data = GsonHelper.customGson.toJson(audioPreview)
                            fos.write(data)
                            fos.flush()
                        }
                    }

                    if (!notify) {
                        callback = null
                        this@OpusAudioRecorder.recordingAudioFile = null
                        return@postRunnable
                    }

                    MixinApplication.get().applicationScope.launch(Dispatchers.Main) {
                        if (endStatus == AudioEndStatus.SEND) {
                            callback?.sendAudio(messageId, recordingAudioFile, duration, waveForm)
                        } else if (endStatus == AudioEndStatus.PREVIEW) {
                            callback?.previewAudio(messageId, recordingAudioFile, duration, waveForm)
                        }
                        callback = null
                        this@OpusAudioRecorder.recordingAudioFile = null
                    }
                },
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
