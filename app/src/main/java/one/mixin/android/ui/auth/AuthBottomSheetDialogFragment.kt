package one.mixin.android.ui.auth

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.DataErrorException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ServerErrorException
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.tip.exception.TipException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.biometric.BiometricDialog
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipType
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.Scope
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@AndroidEntryPoint
class AuthBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AuthBottomSheetDialogFragment"

        const val ARGS_SCOPES = "args_scopes"
        const val ARGS_AUTHORIZATION_ID = "args_authorization_id"

        private const val ARGS_NAME = "args_name"
        private const val ARGS_ICON_URL = "args_icon_url"

        fun newInstance(scopes: ArrayList<Scope>, name: String, iconUrl: String?, authId: String) =
            AuthBottomSheetDialogFragment().withArgs {
                putParcelableArrayList(ARGS_SCOPES, scopes)
                putString(ARGS_NAME, name)
                putString(ARGS_ICON_URL, iconUrl)
                putString(ARGS_AUTHORIZATION_ID, authId)
            }
    }

    private val scopes: List<Scope> by lazy {
        requireArguments().getParcelableArrayList(ARGS_SCOPES)!!
    }

    private val authorizationId: String by lazy {
        requireArguments().getString(ARGS_AUTHORIZATION_ID)!!
    }

    private val appIconUrl: String? by lazy {
        requireArguments().getString(ARGS_ICON_URL)
    }
    private val appName: String by lazy {
        requireArguments().getString(ARGS_NAME)!!
    }

    private var success = false

    private var behavior: BottomSheetBehavior<*>? = null
    override fun getTheme() = R.style.AppTheme_Dialog

    private val bottomViewModel by viewModels<BottomSheetViewModel>()

    private var status by mutableStateOf(Status.DEFAULT)
    private var errorContent by mutableStateOf("")
    private var savedScopes: List<String>? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AuthBottomSheetDialogCompose(
                name = appName,
                iconUrl = appIconUrl,
                scopes = scopes,
                onDismissRequest = {
                    dismiss()
                },
                status = status,
                errorContent = errorContent,
                onResetClick = {
                    status = Status.DEFAULT
                }, onBiometricClick = {
                    savedScopes = it
                    showBiometricPrompt()
                }, onVerifyRequest = { scopes, pin ->
                    authVerify(scopes, pin)
                }
            )
        }
        doOnPreDraw {
            val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
            behavior = params?.behavior as? BottomSheetBehavior<*>
            behavior?.peekHeight = 690.dp
            behavior?.isDraggable = false
            behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
        }
    }

    private fun authVerify(scopes: List<String>, pin: String) = lifecycleScope.launch {
        if (Session.getAccount()?.hasPin != true) {
            TipActivity.show(requireActivity(), TipType.Create)
            return@launch
        }

        try {
            val response = bottomViewModel.authorize(
                authorizationId,
                scopes,
                pin
            )
            if (response.isSuccess) {
                status = Status.DONE
                val data = response.data ?: return@launch
                val redirectUri = data.app.redirectUri
                redirect(redirectUri, data.authorizationCode)
                success = true
                delay(1000)
                dismiss()
            } else {
                val errorInfo =
                    if (response.errorCode == ErrorHandler.PIN_INCORRECT || response.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                        val errorCount = bottomViewModel.errorCount()
                        requireContext().resources.getQuantityString(
                            R.plurals.error_pin_incorrect_with_times,
                            errorCount,
                            errorCount
                        )
                    } else {
                        requireContext().getMixinErrorStringByCode(
                            response.errorCode,
                            response.errorDescription
                        )
                    }
                status = Status.ERROR
                errorContent = errorInfo
            }
        } catch (e: Exception) {
            status = Status.ERROR
            errorContent =
                when (e) {
                    is TipException -> e.getTipExceptionMsg(requireContext())
                    is SocketTimeoutException -> requireContext().getString(R.string.error_connection_timeout)
                    is UnknownHostException -> requireContext().getString(R.string.No_network_connection)
                    is ServerErrorException -> requireContext().getString(
                        R.string.error_server_5xx_code,
                        e.code
                    )
                    is NetworkException -> requireContext().getString(R.string.No_network_connection)
                    is DataErrorException -> requireContext().getString(R.string.Data_error)
                    else -> requireContext().getString(
                        R.string.error_unknown_with_message,
                        e.message
                    )
                }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night)
            )
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    private val bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> dismissAllowingStateLoss()
                else -> {}
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (!success && isAdded) {
            lifecycleScope.launch {
                bottomViewModel // init on main thread
                handleMixinResponse(
                    invokeNetwork = {
                        bottomViewModel.authorize(authorizationId, listOf(), null)
                    },
                    switchContext = Dispatchers.IO,
                    successBlock = {
                        val data = it.data ?: return@handleMixinResponse
                        redirect(data.app.redirectUri, data.authorizationCode)
                    }
                )
            }
        }
        super.onDismiss(dialog)
    }

    private fun redirect(uri: String, code: String?) {
        if (!uri.isWebUrl()) {
            val builder = Uri.parse(uri).buildUpon()
            val redirect = if (code.isNullOrEmpty()) {
                builder.appendQueryParameter("error", "access_denied").build()
            } else {
                builder.appendQueryParameter("code", code).build()
            }
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(redirect.toString())).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        flags = Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
                    }
                }
                context?.startActivity(intent)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private var biometricDialog: BiometricDialog? = null
    private fun showBiometricPrompt() {
        biometricDialog = BiometricDialog(
            requireActivity(),
            getBiometricInfo()
        )
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    fun getBiometricInfo() = BiometricInfo(
        getString(R.string.Verify_by_Biometric),
        "",
        "",
        getString(R.string.Verify_PIN)
    )

    private val biometricDialogCallback = object : BiometricDialog.Callback {
        override fun onPinComplete(pin: String) {
            authVerify(
                requireNotNull(savedScopes),
                pin
            )
        }

        override fun showPin() {
            dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        override fun showAuthenticationScreen() {
            BiometricUtil.showAuthenticationScreen(this@AuthBottomSheetDialogFragment.requireActivity())
        }

        override fun onCancel() {}
    }
}
