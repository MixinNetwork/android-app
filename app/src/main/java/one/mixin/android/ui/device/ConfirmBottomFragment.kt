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
import one.mixin.android.api.ResponseError
import one.mixin.android.api.handleMixinResponse
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
import one.mixin.android.tip.TipBody
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.ui.common.AvatarActivity
import one.mixin.android.ui.oldwallet.biometric.BiometricBottomSheetDialogFragment
import one.mixin.android.ui.oldwallet.biometric.BiometricInfo
import one.mixin.android.ui.oldwallet.biometric.BiometricLayout
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.UnescapeIgnorePlusUrlQuerySanitizer
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import org.whispersystems.libsignal.ecc.Curve
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmBottomFragment : BiometricBottomSheetDialogFragment() {
    companion object {
        const val TAG = "ConfirmBottomFragment"

        private fun newInstance(
            url: String,
            action: ((Boolean, Boolean) -> Unit)? = null,
        ) = ConfirmBottomFragment().withArgs {
            putString(AvatarActivity.ARGS_URL, url)
        }.apply {
            action?.let {
                setCallback(it)
            }
        }

        fun show(
            context: Context,
            fragmentManager: FragmentManager,
            url: String,
            action: ((Boolean, Boolean) -> Unit)? = null,
        ) {
            val uri = Uri.parse(url)
            val ephemeralId: String? =
                try {
                    uri.getQueryParameter("id")
                } catch (e: Exception) {
                    Timber.e("getQueryParameter ${e.stackTraceToString()}")
                    null
                }
            if (ephemeralId == null) {
                toast(R.string.desktop_upgrade)
            } else if (Session.getAccount()?.hasPin == false) {
                context.alert(context.getString(R.string.desktop_login_no_pin))
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
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
        requireNotNull(requireArguments().getString(AvatarActivity.ARGS_URL)) { "required url must not be null" }
    }

    private fun authDevice(
        ephemeralId: String,
        pubKey: String,
        pin: String,
    ) =
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = { provisioningService.provisionCodeAsync() },
                successBlock = { response ->
                    withContext(Dispatchers.IO) {
                        encryptKey(requireContext(), ephemeralId, pubKey, response.data!!.code, pin)
                    }
                },
                failureBlock = {
                    handleError(requireNotNull(it.error))
                    return@handleMixinResponse true
                },
                exceptionBlock = {
                    handleThrowable(it)
                    return@handleMixinResponse true
                },
            )
        }

    private fun handleThrowable(t: Throwable) {
        if (t is TipNetworkException) {
            handleError(requireNotNull(t.error))
        } else {
            showErrorInfo(t.stackTraceToString(), true, errorAction = BiometricLayout.ErrorAction.Close)
            ErrorHandler.handleError(t)
        }
    }

    private fun handleError(error: ResponseError) {
        val errorCode = error.code
        val errorDescription = error.description
        val errorInfo = requireContext().getMixinErrorStringByCode(errorCode, errorDescription)
        showErrorInfo(errorInfo, true, errorAction = BiometricLayout.ErrorAction.Close)
    }

    private val binding by viewBinding(FragmentConfirmBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        setBiometricLayout()
        binding.biometricLayout.apply {
            biometricTv.setText(R.string.Verify_by_Biometric)
        }
    }

    override suspend fun invokeNetwork(pin: String): MixinResponse<*> {
        return MixinResponse(Response.success(Any()))
    }

    override fun doWhenInvokeNetworkSuccess(
        response: MixinResponse<*>,
        pin: String,
    ): Boolean {
        response.data?.let {
            isCancelable = false
            val uri = Uri.parse(url)
            val ephemeralId = uri.getQueryParameter("id")
            if (ephemeralId == null) {
                showErrorInfo("ephemeralId == null", true, errorAction = BiometricLayout.ErrorAction.Close)
                return@let
            }
            sanitizer.parseUrl(url)
            val publicKeyEncoded = sanitizer.getValue("pub_key")
            authDevice(ephemeralId, publicKeyEncoded, pin)
        }
        return false
    }

    override fun onClickBiometricLayoutClose(): Boolean {
        confirmCallback?.invoke(false, true)
        return false
    }

    override fun getBiometricInfo() =
        BiometricInfo(
            getString(R.string.Verify_by_Biometric),
            "",
            "",
        )

    private suspend fun encryptKey(
        ctx: Context,
        ephemeralId: String,
        publicKeyEncoded: String,
        verificationCode: String,
        pin: String,
    ) {
        val account = Session.getAccount() ?: return
        if (TextUtils.isEmpty(ephemeralId) || TextUtils.isEmpty(publicKeyEncoded)) {
            return
        }

        val publicKey = Curve.decodePoint(Base64.decode(publicKeyEncoded), 0)
        val identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(ctx)
        val cipher = ProvisioningCipher(publicKey)
        val message =
            ProvisionMessage(
                identityKeyPair.publicKey.serialize(),
                identityKeyPair.privateKey.serialize(),
                account.userId,
                account.sessionId,
                verificationCode,
            )
        val cipherText = cipher.encrypt(message.toByteArray())
        val encoded = cipherText.base64Encode()
        withContext(Dispatchers.Main) {
            handleMixinResponse(
                invokeNetwork = {
                    provisioningService.updateProvisioningAsync(
                        ephemeralId,
                        ProvisioningRequest(
                            encoded,
                            pinCipher.encryptPin(
                                pin,
                                TipBody.forProvisioningCreate(ephemeralId, encoded),
                            ),
                        ),
                    )
                },
                successBlock = {
                    confirmCallback?.invoke(true, true)
                    toast(R.string.Link_desktop_success)
                    dismiss()
                },
                failureBlock = {
                    confirmCallback?.invoke(false, false)
                    handleWithErrorCodeAndDesc(pin, requireNotNull(it.error))
                    return@handleMixinResponse true
                },
                exceptionBlock = { t ->
                    confirmCallback?.invoke(false, false)
                    handleThrowableWithPin(t, pin)
                    return@handleMixinResponse true
                },
            )
        }
    }

    private val sanitizer =
        UnescapeIgnorePlusUrlQuerySanitizer().apply {
            allowUnregisteredParamaters = true
            unregisteredParameterValueSanitizer =
                UrlQuerySanitizer.IllegalCharacterValueSanitizer(
                    UrlQuerySanitizer.IllegalCharacterValueSanitizer.ALL_OK,
                )
        }

    private var confirmCallback: ((Boolean, Boolean) -> Unit)? = null

    // Boolean, Boolean --- success, complete
    fun setCallback(action: (Boolean, Boolean) -> Unit) {
        confirmCallback = action
    }
}
