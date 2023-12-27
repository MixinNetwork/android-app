package one.mixin.android.extension

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.WebView
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.crypto.Base64
import one.mixin.android.db.MixinDatabase
import one.mixin.android.job.RefreshExternalSchemeJob.Companion.PREF_EXTERNAL_SCHEMES
import one.mixin.android.pay.externalTransferAssetIdMap
import one.mixin.android.session.Session
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.share.ShareMessageBottomSheetDialogFragment
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.vo.App
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.getShareCategory
import timber.log.Timber

fun String.openAsUrlOrWeb(
    context: Context,
    conversationId: String?,
    supportFragmentManager: FragmentManager,
    scope: CoroutineScope,
    app: App? = null,
    appCard: AppCardData? = null,
) = openAsUrl(context, supportFragmentManager, scope, currentConversation = conversationId, app = app) {
    WebActivity.show(context, this, conversationId, app, appCard)
}

fun String.openAsUrlOrQrScan(
    context: Context,
    supportFragmentManager: FragmentManager,
    scope: CoroutineScope,
) = openAsUrl(context, supportFragmentManager, scope) {
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
        startsWith(Constants.Scheme.APPS, true) ||
        startsWith(Constants.Scheme.SNAPSHOTS, true) ||
        startsWith(Constants.Scheme.CONVERSATIONS, true) ||
        startsWith(Constants.Scheme.TIP, true) ||
        startsWith(Constants.Scheme.BUY, true) ||
        startsWith(Constants.Scheme.MIXIN_PAY, true) ||
        startsWith(Constants.Scheme.HTTPS_MULTISIGS, true)
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
        } else {
            startsWith(Constants.Scheme.HTTPS_ADDRESS, true)
        }
    }
}

@SuppressLint("HardwareIds")
fun String.openAsUrl(
    context: Context,
    supportFragmentManager: FragmentManager,
    scope: CoroutineScope,
    currentConversation: String? = null,
    app: App? = null,
    host: String? = null,
    extraAction: () -> Unit,
) {
    if (startsWith(Constants.Scheme.SEND, true)) {
        val uri = Uri.parse(this)
        uri.handleSchemeSend(
            context,
            supportFragmentManager,
            currentConversation,
            app,
            host,
            onError = { err ->
                Timber.e(IllegalStateException(err))
            },
        )
    } else if (startsWith(Constants.Scheme.INFO, true)) {
        val content = """
Brand: ${Build.BRAND} 
App Version: ${BuildConfig.VERSION_NAME}
App ID: ${BuildConfig.APPLICATION_ID}
Device ID: ${Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)} 
Model: ${Build.MODEL} 
ID: ${Build.ID} 
SDK: ${Build.VERSION.SDK_INT}  
Incremental: ${Build.VERSION.INCREMENTAL} 
Version Code: ${Build.VERSION.RELEASE}
User ID: ${Session.getAccountId()}
Google Available: ${context.isGooglePlayServicesAvailable()}
User-agent: ${WebView(context).settings.userAgentString}
"""
        context.alert(content).setPositiveButton(android.R.string.copy) { dialog, _ ->
            context.getClipboardManager().setPrimaryClip(
                ClipData.newPlainText(
                    null,
                    content,
                ),
            )
            toast(R.string.copied_to_clipboard)
            dialog.dismiss()
        }.show()
    } else if (startsWith(Constants.Scheme.DEVICE, true)) {
        ConfirmBottomFragment.show(MixinApplication.appContext, supportFragmentManager, this)
    } else if (isUserScheme() || isAppScheme()) {
        checkUserOrApp(context, supportFragmentManager, scope)
    } else if (startsWith(Constants.Scheme.WALLET_CONNECT_PREFIX) && WalletConnect.isEnabled(context)) {
        WalletConnect.connect(this)
    } else {
        if (isMixinUrl() || isExternalScheme(context) || isExternalTransferUrl()) {
            LinkBottomSheetDialogFragment.newInstance(this)
                .showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        } else {
            extraAction()
        }
    }
}

fun String.isExternalScheme(context: Context): Boolean {
    val externalSchemes = context.defaultSharedPreferences.getStringSet(PREF_EXTERNAL_SCHEMES, emptySet())
    return !externalSchemes.isNullOrEmpty() && this.matchResourcePattern(externalSchemes)
}

fun Uri.checkUserOrApp(
    context: Context,
    supportFragmentManager: FragmentManager,
    scope: CoroutineScope,
) = this.toString().checkUserOrApp(context, supportFragmentManager, scope)

fun String.checkUserOrApp(
    context: Context,
    supportFragmentManager: FragmentManager,
    scope: CoroutineScope,
) {
    val isAppScheme = isAppScheme()
    val ctx = MixinApplication.appContext
    val uri = Uri.parse(this)
    val segments = uri.pathSegments
    if (segments.isEmpty()) return

    val userId =
        if (segments.size >= 2) {
            segments[1]
        } else {
            segments[0]
        }
    if (!userId.isUUID()) {
        toast(getUserOrAppNotFoundTip(isAppScheme))
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
                    val url =
                        try {
                            app.homeUri.appendQueryParamsFromOtherUri(uri)
                        } catch (e: Exception) {
                            app.homeUri
                        }
                    WebActivity.show(context, url, null, app)
                    if (context is UrlInterpreterActivity) {
                        context.finish()
                    }
                    return@launch
                }
            }
            showUserBottom(supportFragmentManager, user)
        }
    }
}

fun String.isExternalTransferUrl() = externalTransferAssetIdMap.keys.any { startsWith("$it:") }

private fun String.isUserScheme() =
    startsWith(Constants.Scheme.USERS, true) ||
        startsWith(Constants.Scheme.HTTPS_USERS, true)

private fun String.isAppScheme() =
    startsWith(Constants.Scheme.APPS, true) ||
        startsWith(Constants.Scheme.HTTPS_APPS, true)

private fun getUserOrAppNotFoundTip(isApp: Boolean) =
    if (isApp) R.string.Bot_not_found else R.string.User_not_found

fun Uri.getRawQueryParameter(key: String): String? {
    val parameters = this.getQueryParameters(key)
    return if (parameters.isEmpty()) {
        null
    } else {
        parameters.first()
    }
}

fun Uri.handleSchemeSend(
    context: Context,
    supportFragmentManager: FragmentManager,
    currentConversation: String? = null,
    app: App? = null,
    host: String? = null,
    showNow: Boolean = true,
    afterShareText: (() -> Unit)? = null,
    afterShareData: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
) {
    val text = this.getQueryParameter("text")
    if (text != null) {
        ForwardActivity.show(
            context,
            arrayListOf(ForwardMessage(ShareCategory.Text, text)),
            ForwardAction.App.Resultless(),
        )
        afterShareText?.invoke()
    } else {
        val category = this.getQueryParameter("category")
        val conversationId =
            this.getQueryParameter("conversation").let {
                if (it == currentConversation) {
                    it
                } else {
                    null
                }
            }
        val data = this.getRawQueryParameter("data")
        val shareCategory = category?.getShareCategory()
        if (shareCategory != null && data != null) {
            try {
                afterShareData?.invoke()
                val fragment =
                    ShareMessageBottomSheetDialogFragment.newInstance(
                        ForwardMessage(shareCategory, String(Base64.decode(data))),
                        conversationId,
                        app,
                        host,
                    )
                if (showNow) {
                    fragment.showNow(supportFragmentManager, ShareMessageBottomSheetDialogFragment.TAG)
                } else {
                    fragment.show(supportFragmentManager, ShareMessageBottomSheetDialogFragment.TAG)
                }
            } catch (e: Exception) {
                onError?.invoke("Error data:${e.message}")
            }
        } else {
            onError?.invoke("Error data")
        }
    }
}

fun Uri.getCapturedImage(contentResolver: ContentResolver): Bitmap =
    when {
        Build.VERSION.SDK_INT < 28 -> {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, this)
        }
        else -> {
            val source = ImageDecoder.createSource(contentResolver, this)
            ImageDecoder.decodeBitmap(source)
        }
    }
