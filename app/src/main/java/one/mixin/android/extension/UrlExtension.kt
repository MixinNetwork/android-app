package one.mixin.android.extension

import android.net.Uri
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.crypto.Base64
import one.mixin.android.db.MixinDatabase
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.common.share.ShareMessageBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.qr.donateSupported
import one.mixin.android.util.Session
import one.mixin.android.vo.App
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage

fun String.openAsUrlOrWeb(
    conversationId: String?,
    supportFragmentManager: FragmentManager,
    scope: CoroutineScope,
    app: App? = null,
    appCard: AppCardData? = null
) = openAsUrl(supportFragmentManager, scope) {
    WebBottomSheetDialogFragment.newInstance(this, conversationId, app, appCard)
        .showNow(supportFragmentManager, WebBottomSheetDialogFragment.TAG)
}

fun String.openAsUrlOrQrScan(
    supportFragmentManager: FragmentManager,
    scope: CoroutineScope
) = openAsUrl(supportFragmentManager, scope) {
    QrScanBottomSheetDialogFragment.newInstance(this)
        .showNow(supportFragmentManager, QrScanBottomSheetDialogFragment.TAG)
}

fun String.isMixinUrl(): Boolean {
    return if (startsWith(Constants.Scheme.HTTPS_PAY, true) ||
        startsWith(Constants.Scheme.PAY, true) ||
        startsWith(Constants.Scheme.USERS, true) ||
        startsWith(Constants.Scheme.HTTPS_USERS, true) ||
        startsWith(Constants.Scheme.DEVICE, true) ||
        startsWith(Constants.Scheme.SEND, true) ||
        startsWith(Constants.Scheme.ADDRESS, true) ||
        startsWith(Constants.Scheme.WITHDRAWAL, true) ||
        startsWith(Constants.Scheme.APPS, true) ||
        startsWith(Constants.Scheme.SNAPSHOTS, true)
    ) {
        true
    } else {
        val segments = Uri.parse(this).pathSegments
        if (startsWith(Constants.Scheme.HTTPS_CODES, true) ||
            startsWith(Constants.Scheme.HTTPS_USERS, true) ||
            startsWith(Constants.Scheme.HTTPS_APPS, true)
        ) {
            segments.size >= 2 && segments[1].isUUID()
        } else if (startsWith(Constants.Scheme.CODES, true) ||
            startsWith(Constants.Scheme.USERS, true) ||
            startsWith(Constants.Scheme.APPS, true)
        ) {
            segments.size >= 1 && segments[0].isUUID()
        } else if (startsWith(Constants.Scheme.TRANSFER, true)) {
            segments.size >= 1 && segments[0].isUUID()
        } else if (startsWith(Constants.Scheme.HTTPS_TRANSFER, true)) {
            segments.size >= 2 && segments[1].isUUID()
        } else if (startsWith(Constants.Scheme.HTTPS_ADDRESS, true)) {
            true
        } else startsWith(Constants.Scheme.HTTPS_WITHDRAWAL, true)
    }
}

fun String.openAsUrl(
    supportFragmentManager: FragmentManager,
    scope: CoroutineScope,
    extraAction: () -> Unit
) {
    if (startsWith(Constants.Scheme.TRANSFER, true) ||
        startsWith(Constants.Scheme.HTTPS_TRANSFER, true)
    ) {
        val segments = Uri.parse(this).pathSegments
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
    } else if (startsWith(Constants.Scheme.SEND, true)) {
        val uri = Uri.parse(this)
        val text = uri.getQueryParameter("text")
        text.notNullWithElse({
            ForwardActivity.show(
                MixinApplication.appContext,
                arrayListOf(ForwardMessage(ForwardCategory.TEXT.name, content = it))
            )
        }, {
            val category = uri.getQueryParameter("category")
            val conversationId = uri.getQueryParameter("conversation_id")
            val data = uri.getQueryParameter("data")
            if (category != null && data != null) {
                try {
                    ShareMessageBottomSheetDialogFragment.newInstance(category, String(Base64.decode(data)))
                        .showNow(supportFragmentManager, ShareMessageBottomSheetDialogFragment.TAG)
                } catch (e: Exception) {
                    extraAction()
                }
            } else {
                extraAction()
            }
        })
    } else if (startsWith(Constants.Scheme.DEVICE, true)) {
        ConfirmBottomFragment.show(MixinApplication.appContext, supportFragmentManager, this)
    } else if (isUserScheme() || isAppScheme()) {
        checkUserOrApp(supportFragmentManager, scope)
    } else {
        if (isMixinUrl() || isDonateUrl()) {
            LinkBottomSheetDialogFragment.newInstance(this)
                .showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        } else {
            extraAction()
        }
    }
}

fun Uri.checkUserOrApp(
    supportFragmentManager: FragmentManager,
    scope: CoroutineScope
) = this.toString().checkUserOrApp(supportFragmentManager, scope)

fun String.checkUserOrApp(
    supportFragmentManager: FragmentManager,
    scope: CoroutineScope
) {
    val isAppScheme = isAppScheme()
    val ctx = MixinApplication.appContext
    val uri = Uri.parse(this)
    val segments = uri.pathSegments
    val userId = if (segments.size >= 2) {
        segments[1]
    } else {
        segments[0]
    }
    if (!userId.isUUID()) {
        ctx.toast(getUserOrAppNotFoundTip(isAppScheme))
        return
    }

    val db = MixinDatabase.getDatabase(ctx)
    val userDao = db.userDao()
    val appDao = db.appDao()
    scope.launch {
        val user = userDao.suspendFindUserById(userId)
        if (user == null) {
            val bottomSheet = LinkBottomSheetDialogFragment.newInstance(uri.toString())
            bottomSheet.showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        } else {
            val isOpenApp = isAppScheme && uri.getQueryParameter("action") == "open"
            if (isOpenApp && user.appId != null) {
                val app = appDao.findAppById(user.appId!!)
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

fun String.isDonateUrl() = donateSupported.any { startsWith(it) }

private fun String.isUserScheme() = startsWith(Constants.Scheme.USERS, true) ||
    startsWith(Constants.Scheme.HTTPS_USERS, true)

private fun String.isAppScheme() = startsWith(Constants.Scheme.APPS, true) ||
    startsWith(Constants.Scheme.HTTPS_APPS, true)

private fun getUserOrAppNotFoundTip(isApp: Boolean) =
    if (isApp) R.string.error_app_not_found else R.string.error_user_not_found
