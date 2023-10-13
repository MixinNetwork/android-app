package one.mixin.android.util.mlkit.scan.manager

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import one.mixin.android.R
import timber.log.Timber
import java.io.Closeable

class BeepManager(private val context: Context) : MediaPlayer.OnErrorListener, Closeable {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var playBeep = false
    private var vibrate = false

    init {
        updatePrefs()
    }

    fun setVibrate(vibrate: Boolean) {
        this.vibrate = vibrate
    }

    fun setPlayBeep(playBeep: Boolean) {
        this.playBeep = playBeep
    }

    @Synchronized
    private fun updatePrefs() {
        if (mediaPlayer == null) {
            mediaPlayer = buildMediaPlayer(context)
        }
        if (vibrator == null) {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @Synchronized
    fun playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer!!.start()
        }
        if (vibrate && vibrator!!.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator!!.vibrate(
                    VibrationEffect.createOneShot(
                        VIBRATE_DURATION,
                        VibrationEffect.DEFAULT_AMPLITUDE,
                    ),
                )
            } else {
                vibrator!!.vibrate(VIBRATE_DURATION)
            }
        }
    }

    private fun buildMediaPlayer(context: Context): MediaPlayer? {
        val mediaPlayer = MediaPlayer()
        return try {
            val file = context.resources.openRawResourceFd(R.raw.beep)
            mediaPlayer.setDataSource(file.fileDescriptor, file.startOffset, file.length)
            mediaPlayer.setOnErrorListener(this)
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer.isLooping = false
            mediaPlayer.prepare()
            mediaPlayer
        } catch (e: Exception) {
            Timber.w(e)
            mediaPlayer.release()
            null
        }
    }

    @Synchronized
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        close()
        updatePrefs()
        return true
    }

    @Synchronized
    override fun close() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer!!.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    companion object {
        private const val VIBRATE_DURATION = 200L
    }
}
