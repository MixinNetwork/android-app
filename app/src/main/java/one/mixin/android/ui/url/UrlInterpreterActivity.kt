package one.mixin.android.ui.url

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.checkUserOrApp
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.util.Session
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage

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
            toast(R.string.not_logged_in)
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
            USER, APPS -> uri.checkUserOrApp(supportFragmentManager, lifecycleScope)
            CODE, PAY, WITHDRAWAL, ADDRESS, SNAPSHOTS -> {
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
                uri.getQueryParameter("text")?.let {
                    ForwardActivity.show(
                        this@UrlInterpreterActivity,
                        arrayListOf(ForwardMessage(ForwardCategory.TEXT.name, content = it))
                    )
                }
                finish()
            }
        }
    }
}
