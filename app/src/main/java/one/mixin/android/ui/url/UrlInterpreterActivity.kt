package one.mixin.android.ui.url

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentManager
import one.mixin.android.Constants.MIXIN_TRANSFER_PREFIX
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

fun isMixinUrl(url: String): Boolean {
    if (url.startsWith("https://mixin.one/pay", true) ||
        url.startsWith("mixin://pay", true) ||
        url.startsWith("mixin://users", true)) {
        return true
    } else if (url.startsWith("https://mixin.one/codes/", true)) {
        val segments = Uri.parse(url).pathSegments
        if (segments.size >= 2) {
            val data = segments[1]
            if (data.isUUID()) {
                return true
            }
        }
        return false
    } else if (url.startsWith("mixin://codes/", true) ||
        url.startsWith("mixin://transfer/", true)) {
        val segments = Uri.parse(url).pathSegments
        if (segments.size >= 1) {
            val data = segments[0]
            if (data.isUUID()) {
                return true
            }
        }
        return false
    } else {
        return false
    }
}

inline fun openUrl(url: String, supportFragmentManager: FragmentManager, extraAction: () -> Unit) {
    val openWithLink = if (url.startsWith("https://mixin.one/pay", true) ||
        url.startsWith("mixin://pay", true) ||
        url.startsWith("mixin://users", true)) {
        true
    } else if (url.startsWith("https://mixin.one/codes/", true)) {
        val segments = Uri.parse(url).pathSegments
        segments.size >= 2 && segments[1].isUUID()
    } else if (url.startsWith("mixin://codes/", true)) {
        val segments = Uri.parse(url).pathSegments
        segments.size >= 1 && segments[0].isUUID()
    } else {
        false
    }

    when {
        url.startsWith(MIXIN_TRANSFER_PREFIX, true) -> {
            val segments = Uri.parse(url).pathSegments
            if (segments.size >= 1) {
                val data = segments[0]
                if (data.isUUID()) {
                    TransferFragment.newInstance(data).showNow(supportFragmentManager, TransferFragment.TAG)
                }
            }
        }
        openWithLink -> LinkBottomSheetDialogFragment
            .newInstance(url)
            .showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        else -> extraAction()
    }
}

fun openWebBottomSheet(url: String, conversationId: String?, supportFragmentManager: FragmentManager) {
    WebBottomSheetDialogFragment
        .newInstance(url, conversationId)
        .showNow(supportFragmentManager, WebBottomSheetDialogFragment.TAG)
}

fun openUrlWithExtraWeb(url: String, conversationId: String?, supportFragmentManager: FragmentManager) =
    openUrl(url, supportFragmentManager) { openWebBottomSheet(url, conversationId, supportFragmentManager) }