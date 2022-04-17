package one.mixin.android.ui.device

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.text.TextUtils
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.ProvisioningRequest
import one.mixin.android.api.service.ProvisioningService
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.IdentityKeyUtil
import one.mixin.android.crypto.ProvisionMessage
import one.mixin.android.crypto.ProvisioningCipher
import one.mixin.android.databinding.FragmentConfirmBinding
import one.mixin.android.extension.alert
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.AvatarActivity
import one.mixin.android.ui.common.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.UnescapeIgnorePlusUrlQuerySanitizer
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import org.whispersystems.libsignal.ecc.Curve
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmBottomFragment : BiometricBottomSheetDialogFragment() {

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
                toast(R.string.desktop_upgrade)
            } else if (Session.getAccount()?.hasPin == false) {
                context.alert(context.getString(R.string.desktop_login_no_pin))
                    .setPositiveButton(android.R.string.yes) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } else {
                newInstance(url, action).showNow(fragmentManager, TAG)
            }
        }
    }

    @Inject
    lateinit var provisioningService: ProvisioningService

    private val url: String by lazy {
        requireArguments().getString(AvatarActivity.ARGS_URL)!!
    }

    private fun authDevice(ephemeralId: String, pubKey: String) = lifecycleScope.launch {
        val response = try {
            withContext(Dispatchers.IO) {
                provisioningService.provisionCodeAsync().await()
            }
        } catch (t: Throwable) {
            toast(R.string.Link_desktop_success)
            ErrorHandler.handleError(t)
            return@launch
        }
        if (response.isSuccess) {
            val success = try {
                withContext(Dispatchers.IO) {
                    encryptKey(requireContext(), ephemeralId, pubKey, response.data!!.code)
                }
            } catch (t: Throwable) {
                toast(R.string.Link_desktop_success)
                ErrorHandler.handleError(t)
                return@launch
            }
            confirmCallback?.invoke()
            if (success) {
                toast(R.string.Link_desktop_failed)
            } else {
                toast(R.string.Link_desktop_success)
            }
            dismiss()
        } else {
            ErrorHandler.handleMixinError(
                response.errorCode,
                response.errorDescription,
                getString(R.string.Link_desktop_success)
            )
            dismiss()
        }
    }

    private val binding by viewBinding(FragmentConfirmBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()
        binding.biometricLayout.apply {
            biometricTv.setText(R.string.Verify_by_Biometric)
            payTv.setText(R.string.login_by_PIN)
        }
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return bottomViewModel.verifyPin(pin)
    }

    override fun doWhenInvokeNetworkSuccess(response: MixinResponse<*>, pin: String): Boolean {
        response.data?.let {
            isCancelable = false
            val uri = Uri.parse(url)
            val ephemeralId = uri.getQueryParameter("id")
            if (ephemeralId == null) {
                toast(R.string.Link_desktop_success)
                dismiss()
                return@let
            }
            sanitizer.parseUrl(url)
            val publicKeyEncoded = sanitizer.getValue("pub_key")
            authDevice(ephemeralId, publicKeyEncoded)
        }
        return false
    }

    override fun getBiometricInfo() = BiometricInfo(
        getString(R.string.Verify_by_Biometric),
        "",
        "",
        getString(R.string.login_by_PIN)
    )

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
            account.sessionId,
            verificationCode
        )
        val cipherText = cipher.encrypt(message.toByteArray())
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
