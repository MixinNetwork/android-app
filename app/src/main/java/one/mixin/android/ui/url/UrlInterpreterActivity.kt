package one.mixin.android.ui.url

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentManager
import one.mixin.android.Constants.Scheme
import one.mixin.android.R
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.util.Session

class UrlInterpreterActivity : BaseActivity() {
    companion object {
        private const val CODE = "codes"
        private const val PAY = "pay"
        private const val USER = "users"
        private const val TRANSFER = "transfer"
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
            CODE, PAY, USER -> {
                val bottomSheet = LinkBottomSheetDialogFragment.newInstance(uri.toString())
                bottomSheet.showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
            }
            TRANSFER -> {
                TransferFragment.newInstance(uri.lastPathSegment).showNow(supportFragmentManager, TransferFragment.TAG)
            }
        }
    }
}

fun isMixinUrl(url: String, includeTransfer: Boolean = true): Boolean {
    return if (url.startsWith(Scheme.HTTPS_PAY, true)
        || url.startsWith(Scheme.PAY, true)
        || url.startsWith(Scheme.USERS, true)
        || url.startsWith(Scheme.HTTPS_USERS, true)) {
        true
    } else {
        val segments = Uri.parse(url).pathSegments
        if (url.startsWith(Scheme.HTTPS_CODES, true)) {
            segments.size >= 2 && segments[1].isUUID()
        } else if (url.startsWith(Scheme.CODES, true)) {
            segments.size >= 1 && segments[0].isUUID()
        } else if (includeTransfer && url.startsWith(Scheme.TRANSFER, true)) {
            segments.size >= 1 && segments[0].isUUID()
        } else if (includeTransfer && url.startsWith(Scheme.HTTPS_TRANSFER, true)) {
            segments.size >= 2 && segments[1].isUUID()
        } else {
            false
        }
    }
}

inline fun openUrl(url: String, supportFragmentManager: FragmentManager, extraAction: () -> Unit) {
    if (url.startsWith(Scheme.TRANSFER, true)) {
        val segments = Uri.parse(url).pathSegments
        if (segments.size >= 1) {
            val data = segments[0]
            if (data.isUUID()) {
                TransferFragment.newInstance(data).showNow(supportFragmentManager, TransferFragment.TAG)
            }
        }
    } else {
        if (isMixinUrl(url, false)) {
            LinkBottomSheetDialogFragment
                .newInstance(url)
                .showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        } else {
            extraAction()
        }
    }
}

fun openWebBottomSheet(url: String, conversationId: String?, supportFragmentManager: FragmentManager) {
    WebBottomSheetDialogFragment
        .newInstance(url, conversationId)
        .showNow(supportFragmentManager, WebBottomSheetDialogFragment.TAG)
}

fun openUrlWithExtraWeb(url: String, conversationId: String?, supportFragmentManager: FragmentManager) =
    openUrl(url, supportFragmentManager) { openWebBottomSheet(url, conversationId, supportFragmentManager) }