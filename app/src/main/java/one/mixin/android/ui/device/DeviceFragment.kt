package one.mixin.android.ui.device

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.method.LinkMovementMethod
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.service.UserService
import one.mixin.android.databinding.FragmentDeviceBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.highLightClick
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.session.Session.PREF_EXTENSION_SESSION_ID
import one.mixin.android.session.Session.PREF_SESSION
import one.mixin.android.ui.common.AvatarActivity.Companion.ARGS_URL
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import one.mixin.android.ui.setting.LogoutPinBottomSheetDialogFragment
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import javax.inject.Inject

@AndroidEntryPoint
class DeviceFragment() : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DeviceFragment"

        fun newInstance(url: String? = null) =
            DeviceFragment().withArgs {
                if (url != null) {
                    putString(ARGS_URL, url)
                }
            }
    }

    override val centerOnExpandedScreen = true

    @Inject
    lateinit var userService: UserService

    private var disposable: Disposable? = null

    private var loggedIn = false

    private val sessionPref =
        MixinApplication.appContext.sharedPreferences(PREF_SESSION)

    private val sessionListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == PREF_EXTENSION_SESSION_ID) {
                updateUI(sp.getString(key, null) != null)
            }
        }

    // for testing
    private lateinit var resultRegistry: ActivityResultRegistry

    // testing constructor
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    constructor(
        testRegistry: ActivityResultRegistry,
    ) : this() {
        resultRegistry = testRegistry
    }

    // for testing
    var scanResult: String? = null

    lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!::resultRegistry.isInitialized) resultRegistry = requireActivity().activityResultRegistry

        getScanResult = registerForActivityResult(CaptureActivity.CaptureContract(), resultRegistry, ::callbackScan)
    }

    private val binding by viewBinding(FragmentDeviceBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.doOnPreDraw {
            binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
                height = binding.root.getSafeAreaInsetsTop()
            }
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.titleView.leftIb.setOnClickListener { dismiss() }
        binding.titleView.rightIb.setOnClickListener { showHelp() }
        binding.scanBtn.apply {
            doOnLayout {
                val contentWidth = paint.measureText(text.toString()) + compoundDrawablePadding + 20.dp
                val horizontalPadding = ((width - contentWidth) / 2).toInt().coerceAtLeast(0)
                setPaddingRelative(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)
            }
            setOnClickListener {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.CAMERA)
                    .autoDispose(stopScope)
                    .subscribe { granted ->
                        if (granted) {
                            getScanResult.launch(Pair(ARGS_FOR_SCAN_RESULT, true))
                        } else {
                            context?.openPermissionSetting()
                        }
                    }
            }
        }
        binding.logoutBtn.setOnClickListener {
            loadOuting.show()
            lifecycleScope.launch {
                val sessionId = Session.getExtensionSessionId()
                if (sessionId == null) {
                    loadOuting.dismiss()
                    toast(R.string.setting_desktop_logout_failed)
                    return@launch
                }
                LogoutPinBottomSheetDialogFragment.newInstance(sessionId)
                    .apply {
                        setOnSuccess { isSuccess ->
                            if (isSuccess) {
                                loadOuting.dismiss()
                                updateUI(false)
                            } else {
                                loadOuting.dismiss()
                            }
                        }
                    }
                    .showNow(parentFragmentManager, LogoutPinBottomSheetDialogFragment.TAG)
            }
        }
    }

    private fun showHelp() {
        DesktopHelpBottomSheetDialogFragment.newInstance()
            .showNow(parentFragmentManager, DesktopHelpBottomSheetDialogFragment.TAG)
    }

    private fun callbackScan(data: Intent?) {
        val url = data?.getStringExtra(ARGS_FOR_SCAN_RESULT)
        scanResult = url
        url?.let {
            confirm(it)
        }
    }

    override fun onResume() {
        super.onResume()
        checkSession()
        sessionPref.registerOnSharedPreferenceChangeListener(sessionListener)
    }

    override fun onPause() {
        super.onPause()
        sessionPref.unregisterOnSharedPreferenceChangeListener(sessionListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

    private fun checkSession() {
        val sessionId = Session.getExtensionSessionId()
        updateUI(sessionId != null)
    }

    private fun updateUI(loggedIn: Boolean) {
        this.loggedIn = loggedIn
        binding.apply {
            logoIv.setImageResource(if (loggedIn) R.drawable.ic_desktop_linked else R.drawable.ic_desktop_link)
            desktopTitleTv.setText(if (loggedIn) R.string.Mixin_Messenger_Desktop else R.string.desktop_intro_title)
            descTv.isVisible = !loggedIn
            scanBtn.isVisible = !loggedIn
            statusTv.isVisible = loggedIn
            logoutBtn.isVisible = loggedIn
            platformTv.isVisible = false
        }
        if (loggedIn) {
            loadPlatform()
        } else {
            val learnMore = getString(R.string.Learn_More)
            binding.descTv.apply {
                text = getString(R.string.desktop_intro_desc) + " " + learnMore
                highLightClick(learnMore, color = requireContext().getColor(R.color.colorBlue)) { showHelp() }
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    private fun loadPlatform() {
        val sessionId = Session.getExtensionSessionId() ?: return
        val accountId = Session.getAccountId() ?: return
        lifecycleScope.launch {
            val platform =
                handleMixinResponse(
                    invokeNetwork = { userService.fetchSessionsSuspend(listOf(accountId)) },
                    successBlock = { response ->
                        response.data?.firstOrNull { it.sessionId == sessionId }?.platform
                    },
                    failureBlock = { true },
                    exceptionBlock = { true },
                )
            if (!loggedIn || platform.isNullOrBlank()) return@launch
            binding.platformTv.text = platform
            binding.platformTv.isVisible = true
        }
    }

    private val loadOuting: Dialog by lazy {
        indeterminateProgressDialog(
            message = R.string.Please_wait_a_bit,
            title = R.string.Logout,
        ).apply {
            setCancelable(false)
        }
    }

    private fun confirm(url: String) {
        ConfirmBottomFragment.show(requireContext(), childFragmentManager, url) { success, _ ->
            if (success) {
                updateUI(true)
            }
        }
    }
}
