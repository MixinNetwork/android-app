package one.mixin.android.ui.device

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import kotlinx.android.synthetic.main.fragment_confirm.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.request.ProvisioningRequest
import one.mixin.android.api.service.ProvisioningService
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.IdentityKeyUtil
import one.mixin.android.crypto.ProfileKeyUtil
import one.mixin.android.crypto.ProvisionMessage
import one.mixin.android.crypto.ProvisioningCipher
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.AvatarActivity
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.util.UnescapeIgnorePlusUrlQuerySanitizer
import one.mixin.android.widget.BottomSheet
import org.whispersystems.libsignal.ecc.Curve
import javax.inject.Inject

class ConfirmBottomFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "ConfirmBottomFragment"

        fun newInstance(url: String) = ConfirmBottomFragment().withArgs {
            putString(AvatarActivity.ARGS_URL, url)
        }
    }

    @Inject
    lateinit var provisioningService: ProvisioningService

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        ErrorHandler.handleError(exception)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_confirm, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    private fun login() {
        val url = arguments!!.getString(AvatarActivity.ARGS_URL)!!
        GlobalScope.launch(coroutineExceptionHandler) {
            val response = provisioningService.provisionCodeAsync().await()
            if (response.isSuccess) {
                val success = encryptKey(requireContext(), url, response.data!!.code)
                withContext(Dispatchers.Main) {
                    confirmCallback?.invoke()
                    if (success) {
                        context?.toast(R.string.setting_desktop_sigin_success)
                    } else {
                        context?.toast(R.string.setting_desktop_sigin_failed)
                    }
                    dismiss()
                }
            } else {
                withContext(Dispatchers.Main) {
                    context?.toast(R.string.setting_desktop_sigin_failed)
                    dismiss()
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.confirm.setOnClickListener {
            contentView.progress.visibility = View.VISIBLE
            contentView.confirm.visibility = View.INVISIBLE
            contentView.cancel.visibility = View.INVISIBLE
            isCancelable = false
            login()
        }
        contentView.title_view.right_iv.setOnClickListener {
            dismiss()
        }
        contentView.cancel.setOnClickListener {
            dismiss()
        }
    }

    private suspend fun encryptKey(
        ctx: Context,
        url: String,
        verificationCode: String
    ): Boolean {
        val account = Session.getAccount() ?: return false
        val uri = Uri.parse(url)
        if (uri.scheme != "mixin") {
            return false
        }
        val ephemeralId = uri.getQueryParameter("uuid") ?: return false
        sanitizer.parseUrl(url)
        val publicKeyEncoded = sanitizer.getValue("pub_key")

        if (TextUtils.isEmpty(ephemeralId) || TextUtils.isEmpty(publicKeyEncoded)) {
            return false
        }

        val publicKey = Curve.decodePoint(Base64.decode(publicKeyEncoded), 0)
        val identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(ctx)
        val profileKey = ProfileKeyUtil.getProfileKey(ctx)
        val cipher = ProvisioningCipher(publicKey)
        val message = ProvisionMessage(identityKeyPair.publicKey.serialize(), identityKeyPair.privateKey.serialize(), account.userId, account.session_id, verificationCode, profileKey)
        val cipherText = cipher.encrypt(message)
        val encoded = Base64.encodeBytes(cipherText)
        val response = provisioningService.updateProvisioningAsync(ephemeralId, ProvisioningRequest(encoded)).await()
        return response.isSuccess
    }

    private val sanitizer = UnescapeIgnorePlusUrlQuerySanitizer().apply {
        allowUnregisteredParamaters = true
        unregisteredParameterValueSanitizer = UrlQuerySanitizer.IllegalCharacterValueSanitizer(
            UrlQuerySanitizer.IllegalCharacterValueSanitizer.ALL_OK)
    }

    private var confirmCallback: (() -> Unit)? = null
    fun setCallBack(action: () -> Unit) {
        confirmCallback = action
    }
}