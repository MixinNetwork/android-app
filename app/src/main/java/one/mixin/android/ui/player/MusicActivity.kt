package one.mixin.android.ui.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.WindowManager
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.webrtc.EXTRA_CONVERSATION_ID
import timber.log.Timber

@AndroidEntryPoint
class MusicActivity : BaseActivity() {

    companion object {
        fun show(context: Context, conversationId: String) {
            val intent = Intent(context, MusicActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
            try {
                pendingIntent.send()
            } catch (e: PendingIntent.CanceledException) {
                Timber.w(e)
            }
        }
    }

    override fun getDefaultThemeId(): Int {
        return R.style.AppTheme_Transparent
    }

    override fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_Transparent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        volumeControlStream = AudioManager.STREAM_MUSIC

        var conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        if (conversationId == null) {
            conversationId = FloatingPlayer.getInstance().conversationId
        }
        if (conversationId == null) {
            finish()
            return
        }
        MusicBottomSheetDialogFragment.newInstance(conversationId)
            .showNow(supportFragmentManager, MusicBottomSheetDialogFragment.TAG)

        handleIntent()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent()
    }

    var serviceStopped = false

    override fun finish() {
        if (!serviceStopped) {
            collapse(this)
        }
        super.finish()
    }

    private fun handleIntent() {
        FloatingPlayer.getInstance().hide()
    }
}
