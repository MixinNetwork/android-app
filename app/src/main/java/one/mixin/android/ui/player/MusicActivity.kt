package one.mixin.android.ui.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.checkInlinePermissions
import one.mixin.android.extension.showPipPermissionNotification
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
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
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
    }

    var serviceStopped = false
    private var setClicked = false

    override fun onStop() {
        super.onStop()
        if (!serviceStopped) {
            if (!checkFloatingPermission()) {
                if (!setClicked) {
                    showPipPermissionNotification(MusicActivity::class.java, getString(R.string.web_floating_permission))
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private var permissionAlert: AlertDialog? = null

    fun checkFloatingPermission() =
        checkInlinePermissions {
            if (setClicked) {
                setClicked = false
                return@checkInlinePermissions
            }
            if (permissionAlert != null && permissionAlert!!.isShowing) return@checkInlinePermissions

            permissionAlert = AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.web_floating_permission)
                .setPositiveButton(R.string.Setting) { dialog, _ ->
                    try {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                    dialog.dismiss()
                    setClicked = true
                }.show()
        }
}
