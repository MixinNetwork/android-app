package one.mixin.android.ui.url

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.checkUserOrApp
import one.mixin.android.extension.handleSchemeSend
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.device.ConfirmBottomFragment
import timber.log.Timber
import kotlin.IllegalStateException

@AndroidEntryPoint
class UrlInterpreterActivity : BaseActivity() {
    companion object {
        private const val CODE = "codes"
        private const val PAY = "pay"
        private const val USER = "users"
        private const val TRANSFER = "transfer"
        private const val DEVICE = "device"
        private const val SEND = "send"
        private const val WITHDRAWAL = "withdrawal"
        private const val ADDRESS = "address"
        private const val APPS = "apps"
        private const val SNAPSHOTS = "snapshots"
        private const val CONVERSATIONS = "conversations"

        fun show(context: Context, data: Uri) {
            Intent(context, UrlInterpreterActivity::class.java).apply {
                setData(data)
                context.startActivity(this)
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
        val data = intent.data
        if (data == null) {
            finish()
            return
        }
        if (Session.getAccount() == null) {
            toast(R.string.Not_logged_in)
            finish()
            return
        }
        interpretIntent(data)
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun interpretIntent(uri: Uri) {
        when (uri.host) {
            USER, APPS -> uri.checkUserOrApp(this, supportFragmentManager, lifecycleScope)
            CODE, PAY, WITHDRAWAL, ADDRESS, SNAPSHOTS, CONVERSATIONS -> {
                val bottomSheet = LinkBottomSheetDialogFragment.newInstance(uri.toString())
                bottomSheet.showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
            }
            TRANSFER -> {
                uri.lastPathSegment?.let { lastPathSegment ->
                    if (Session.getAccount()?.hasPin == true) {
                        TransferFragment.newInstance(lastPathSegment, supportSwitchAsset = true)
                            .showNow(supportFragmentManager, TransferFragment.TAG)
                    } else {
                        toast(R.string.transfer_without_pin)
                    }
                }
            }
            DEVICE -> {
                ConfirmBottomFragment.show(this, supportFragmentManager, uri.toString())
            }
            SEND -> {
                uri.handleSchemeSend(
                    this,
                    supportFragmentManager,
                    afterShareText = { finish() },
                    onError = { err ->
                        Timber.e(IllegalStateException(err))
                    }
                )
            }
        }
    }
}
