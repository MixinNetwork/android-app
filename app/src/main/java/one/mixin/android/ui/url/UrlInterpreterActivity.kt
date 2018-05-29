package one.mixin.android.ui.url

import android.content.UriMatcher
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentManager
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.util.Session

class UrlInterpreterActivity : BaseActivity() {
    companion object {
        private const val CODE = 100
        private const val PAY = 101
        private const val USER = 102
        private const val TRANSFER = 103
        private val sURIMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI("codes", "*", CODE)
            addURI("pay", null, PAY)
            addURI("users", null, USER)
            addURI("transfer", null, TRANSFER)
        }
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
        when (sURIMatcher.match(uri)) {
            CODE, PAY, USER -> {
                val bottomSheet = LinkBottomSheetDialogFragment.newInstance(uri.toString())
                bottomSheet.show(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
            }
            TRANSFER -> {
                supportFragmentManager.inTransaction {
                    setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                        R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                        .add(R.id.container, TransferFragment.newInstance(uri.lastPathSegment), TransferFragment.TAG)
                        .addToBackStack(null)
                }
            }
            else -> {
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

fun openUrl(url: String, conversationId: String?, supportFragmentManager: FragmentManager) {
    if (url.startsWith(Constants.MIXIN_TRANSFER_PREFIX, true)) {
        val uri = Uri.parse(url)
        val userId = uri.lastPathSegment
        supportFragmentManager.inTransaction {
            setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                .add(R.id.container, TransferFragment.newInstance(userId), TransferFragment.TAG)
                .addToBackStack(null)
        }
        return
    }
    when {
        isMixinUrl(url) -> LinkBottomSheetDialogFragment
            .newInstance(url)
            .showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        else -> WebBottomSheetDialogFragment
            .newInstance(url, conversationId)
            .showNow(supportFragmentManager, WebBottomSheetDialogFragment.TAG)
    }
}