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
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.Scope
import timber.log.Timber

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
                scopes,
                {
                    dismiss()
                }, { scopes ->
                PinInputBottomSheetDialogFragment.newInstance(disableBiometric = false)
                    .let { pinInput ->
                        pinInput.setOnPinComplete { pin ->
                            lifecycleScope.launch {
                                bottomViewModel // init on main thread
                                handleMixinResponse(
                                    invokeNetwork = {
                                        bottomViewModel.authorize(
                                            authorizationId,
                                            scopes.map { it.source },
                                            pin
                                        )
                                    },
                                    switchContext = Dispatchers.IO,
                                    successBlock = {
                                        val data = it.data ?: return@handleMixinResponse
                                        val redirectUri = data.app.redirectUri
                                        redirect(redirectUri, data.authorizationCode)
                                        success = true
                                        pinInput.dismiss()
                                        dismiss()
                                    },
                                    doAfterNetworkSuccess = {
                                    },
                                    exceptionBlock = {
                                        pinInput.dismiss()
                                        return@handleMixinResponse false
                                    }
                                )
                            }
                        }
                    }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
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
}
