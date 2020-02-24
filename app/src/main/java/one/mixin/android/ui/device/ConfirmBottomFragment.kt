package one.mixin.android.ui.device

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_confirm.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.request.ProvisioningRequest
import one.mixin.android.api.service.ProvisioningService
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.IdentityKeyUtil
import one.mixin.android.crypto.ProvisionMessage
import one.mixin.android.crypto.ProvisioningCipher
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.AvatarActivity
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.util.UnescapeIgnorePlusUrlQuerySanitizer
import one.mixin.android.widget.BottomSheet
import org.whispersystems.libsignal.ecc.Curve

class ConfirmBottomFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "ConfirmBottomFragment"

        private fun newInstance(
            url: String,
            action: (() -> Unit)? = null
        ) = ConfirmBottomFragment().withArgs {
            putString(AvatarActivity.ARGS_URL, url)
        }.apply {
            action?.let {
                setCallBack(it)
            }
        }

        fun show(
            context: Context,
            fragmentManager: FragmentManager,
            url: String,
            action: (() -> Unit)? = null
        ) {
            val uri = Uri.parse(url)
            val ephemeralId = uri.getQueryParameter("id")
            if (ephemeralId == null) {
                context.toast(R.string.desktop_upgrade)
            } else {
                newInstance(url, action).showNow(fragmentManager, TAG)
            }
        }
    }

    @Inject
    lateinit var provisioningService: ProvisioningService

    private val url: String by lazy {
        arguments!!.getString(AvatarActivity.ARGS_URL)!!
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_confirm, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    private fun authDevice(ephemeralId: String, pubKey: String) = lifecycleScope.launch {
        val response = try {
            withContext(Dispatchers.IO) {
                provisioningService.provisionCodeAsync().await()
            }
        } catch (t: Throwable) {
            context?.toast(R.string.setting_desktop_sigin_failed)
            refreshUI(false)
            ErrorHandler.handleError(t)
            return@launch
        }
        if (response.isSuccess) {
            val success = try {
                withContext(Dispatchers.IO) {
                    encryptKey(requireContext(), ephemeralId, pubKey, response.data!!.code)
                }
            } catch (t: Throwable) {
                context?.toast(R.string.setting_desktop_sigin_failed)
                refreshUI(false)
                ErrorHandler.handleError(t)
                return@launch
            }
            confirmCallback?.invoke()
            if (success) {
                context?.toast(R.string.setting_desktop_sigin_success)
            } else {
                context?.toast(R.string.setting_desktop_sigin_failed)
            }
            dismiss()
        } else {
            context?.toast(R.string.setting_desktop_sigin_failed)
            dismiss()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.confirm.setOnClickListener {
            refreshUI(true)
            isCancelable = false
            val uri = Uri.parse(url)
            val ephemeralId = uri.getQueryParameter("id")
            if (ephemeralId == null) {
                context?.toast(R.string.setting_desktop_sigin_failed)
                dismiss()
                return@setOnClickListener
            }
            sanitizer.parseUrl(url)
            val publicKeyEncoded = sanitizer.getValue("pub_key")
            authDevice(ephemeralId, publicKeyEncoded)
        }
        contentView.close.setOnClickListener {
            dismiss()
        }
        contentView.cancel.setOnClickListener {
            dismiss()
        }
    }

    private fun refreshUI(showPb: Boolean) {
        if (!isAdded) return
        contentView.progress.isVisible = showPb
        contentView.confirm.isInvisible = showPb
        contentView.cancel.isInvisible = showPb
        contentView.close.isInvisible = showPb
    }

    private suspend fun encryptKey(
        ctx: Context,
        ephemeralId: String,
        publicKeyEncoded: String,
        verificationCode: String
    ): Boolean {
        val account = Session.getAccount() ?: return false
        if (TextUtils.isEmpty(ephemeralId) || TextUtils.isEmpty(publicKeyEncoded)) {
            return false
        }

        val publicKey = Curve.decodePoint(Base64.decode(publicKeyEncoded), 0)
        val identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(ctx)
        val cipher = ProvisioningCipher(publicKey)
        val message = ProvisionMessage(
            identityKeyPair.publicKey.serialize(),
            identityKeyPair.privateKey.serialize(),
            account.userId,
            account.session_id,
            verificationCode
        )
        val cipherText = cipher.encrypt(message)
        val encoded = cipherText.base64Encode()
        val response =
            provisioningService.updateProvisioningAsync(ephemeralId, ProvisioningRequest(encoded))
                .await()
        return response.isSuccess
    }

    private val sanitizer = UnescapeIgnorePlusUrlQuerySanitizer().apply {
        allowUnregisteredParamaters = true
        unregisteredParameterValueSanitizer = UrlQuerySanitizer.IllegalCharacterValueSanitizer(
            UrlQuerySanitizer.IllegalCharacterValueSanitizer.ALL_OK
        )
    }

    private var confirmCallback: (() -> Unit)? = null
    fun setCallBack(action: () -> Unit) {
        confirmCallback = action
    }
}
