package one.mixin.android.ui.url

import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Scheme
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.util.Session
import one.mixin.android.vo.App
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage

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

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val urlViewModel: UrlInterpreterViewModel by viewModels { viewModelFactory }

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
            USER, APPS -> checkUserOrApp(uri)
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

    private fun checkUserOrApp(uri: Uri) = lifecycleScope.launch {
        val url = uri.toString()
        val isUserScheme =
            url.startsWith(Scheme.USERS, true) || url.startsWith(Scheme.HTTPS_USERS, true)
        val isAppScheme =
            url.startsWith(Scheme.APPS, true) || url.startsWith(Scheme.HTTPS_APPS, true)
        if (isUserScheme || isAppScheme) {
            val segments = uri.pathSegments
            val userId = if (segments.size >= 2) {
                segments[1]
            } else {
                segments[0]
            }
            if (!userId.isUUID()) {
                toast(getUserOrAppNotFoundTip(isAppScheme))
                return@launch
            }
            val user = urlViewModel.suspendFindUserById(userId)
            if (user == null) {
                val bottomSheet = LinkBottomSheetDialogFragment.newInstance(uri.toString())
                bottomSheet.showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
            } else {
                val isOpenApp = isAppScheme && uri.getQueryParameter("action") == "open"
                if (isOpenApp && user.appId != null) {
                    val app = urlViewModel.findAppById(user.appId!!)
                    if (app != null) {
                        WebBottomSheetDialogFragment.newInstance(app.homeUri, null, app)
                            .showNow(supportFragmentManager, WebBottomSheetDialogFragment.TAG)
                        return@launch
                    }
                }
                UserBottomSheetDialogFragment.newInstance(user)
                    .showNow(supportFragmentManager, UserBottomSheetDialogFragment.TAG)
            }
        }
    }
}

private fun getUserOrAppNotFoundTip(isApp: Boolean) =
    if (isApp) R.string.error_app_not_found else R.string.error_user_not_found

fun isMixinUrl(url: String): Boolean {
    return if (url.startsWith(Scheme.HTTPS_PAY, true) ||
        url.startsWith(Scheme.PAY, true) ||
        url.startsWith(Scheme.USERS, true) ||
        url.startsWith(Scheme.HTTPS_USERS, true) ||
        url.startsWith(Scheme.DEVICE, true) ||
        url.startsWith(Scheme.SEND, true) ||
        url.startsWith(Scheme.ADDRESS, true) ||
        url.startsWith(Scheme.WITHDRAWAL, true) ||
        url.startsWith(Scheme.APPS, true) ||
        url.startsWith(Scheme.SNAPSHOTS, true)
    ) {
        true
    } else {
        val segments = Uri.parse(url).pathSegments
        if (url.startsWith(Scheme.HTTPS_CODES, true) ||
            url.startsWith(Scheme.HTTPS_USERS, true) ||
            url.startsWith(Scheme.HTTPS_APPS, true)
        ) {
            segments.size >= 2 && segments[1].isUUID()
        } else if (url.startsWith(Scheme.CODES, true) ||
            url.startsWith(Scheme.USERS, true) ||
            url.startsWith(Scheme.APPS, true)
        ) {
            segments.size >= 1 && segments[0].isUUID()
        } else if (url.startsWith(Scheme.TRANSFER, true)) {
            segments.size >= 1 && segments[0].isUUID()
        } else if (url.startsWith(Scheme.HTTPS_TRANSFER, true)) {
            segments.size >= 2 && segments[1].isUUID()
        } else if (url.startsWith(Scheme.HTTPS_ADDRESS, true)) {
            true
        } else url.startsWith(Scheme.HTTPS_WITHDRAWAL, true)
    }
}

inline fun openUrl(url: String, supportFragmentManager: FragmentManager, extraAction: () -> Unit) {
    if (url.startsWith(Scheme.TRANSFER, true) || url.startsWith(Scheme.HTTPS_TRANSFER, true)) {
        val segments = Uri.parse(url).pathSegments
        val data = when {
            segments.size >= 2 -> segments[1]
            segments.size >= 1 -> segments[0]
            else -> ""
        }
        if (data.isUUID()) {
            if (Session.getAccount()?.hasPin == true) {
                TransferFragment.newInstance(data, supportSwitchAsset = true)
                    .showNow(supportFragmentManager, TransferFragment.TAG)
            } else {
                MixinApplication.appContext.toast(R.string.transfer_without_pin)
            }
        }
    } else if (url.startsWith(Scheme.SEND, true)) {
        Uri.parse(url).getQueryParameter("text")?.let {
            ForwardActivity.show(
                MixinApplication.appContext,
                arrayListOf(ForwardMessage(ForwardCategory.TEXT.name, content = it))
            )
        }
    } else if (url.startsWith(Scheme.DEVICE, true)) {
        ConfirmBottomFragment.show(MixinApplication.appContext, supportFragmentManager, url)
    } else {
        if (isMixinUrl(url)) {
            LinkBottomSheetDialogFragment
                .newInstance(url)
                .showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        } else {
            extraAction()
        }
    }
}

fun openWebBottomSheet(
    url: String,
    conversationId: String?,
    app: App? = null,
    supportFragmentManager: FragmentManager,
    onDismiss: (() -> Unit)? = null
) {
    val dialog = WebBottomSheetDialogFragment.newInstance(url, conversationId, app)
    onDismiss?.let { dismiss ->
        dialog.dialog?.setOnDismissListener {
            dismiss.invoke()
        }
    }
    dialog.showNow(supportFragmentManager, WebBottomSheetDialogFragment.TAG)
}

fun openUrlWithExtraWeb(
    url: String,
    conversationId: String?,
    supportFragmentManager: FragmentManager,
    app: App? = null,
    onDismiss: (() -> Unit)? = null
) = openUrl(url, supportFragmentManager) {
    openWebBottomSheet(
        url,
        conversationId,
        app,
        supportFragmentManager = supportFragmentManager,
        onDismiss = onDismiss
    )
}
