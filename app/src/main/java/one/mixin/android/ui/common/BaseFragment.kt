package one.mixin.android.ui.common

import android.support.v4.app.Fragment
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import one.mixin.android.di.Injectable
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.url.isMixinUrl

open class BaseFragment : Fragment(), Injectable {
    protected val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }
    open fun onBackPressed() = false

    protected fun openUrl(url: String, conversationId: String) {
        when {
            isMixinUrl(url) -> LinkBottomSheetDialogFragment
                .newInstance(url)
                .showNow(fragmentManager, LinkBottomSheetDialogFragment.TAG)
            else -> WebBottomSheetDialogFragment
                .newInstance(url, conversationId)
                .showNow(fragmentManager, WebBottomSheetDialogFragment.TAG)
        }
    }
}