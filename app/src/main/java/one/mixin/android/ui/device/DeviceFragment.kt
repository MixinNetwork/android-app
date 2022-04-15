package one.mixin.android.ui.device

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDeviceBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.textColor
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.session.Session.PREF_EXTENSION_SESSION_ID
import one.mixin.android.session.Session.PREF_SESSION
import one.mixin.android.ui.common.AvatarActivity.Companion.ARGS_URL
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DeviceFragment() : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DeviceFragment"

        fun newInstance(url: String? = null) = DeviceFragment().withArgs {
            if (url != null) {
                putString(ARGS_URL, url)
            }
        }
    }

    private var disposable: Disposable? = null

    private var loggedIn = false

    private val sessionPref =
        MixinApplication.appContext.sharedPreferences(PREF_SESSION)

    private val sessionListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
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
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.titleView.leftIb.setOnClickListener { dismiss() }
        binding.authTv.setOnClickListener {
            if (loggedIn) {
                loadOuting.show()
                lifecycleScope.launch {
                    val sessionId = Session.getExtensionSessionId()
                    if (sessionId == null) {
                        loadOuting.dismiss()
                        toast(R.string.setting_desktop_logout_failed)
                        return@launch
                    }
                    val response = try {
                        bottomViewModel.logout(sessionId)
                    } catch (t: Throwable) {
                        loadOuting.dismiss()
                        toast(R.string.setting_desktop_logout_failed)
                        ErrorHandler.handleError(t)
                        return@launch
                    }
                    if (response.isSuccess) {
                        loadOuting.dismiss()
                        updateUI(false)
                    } else {
                        loadOuting.dismiss()
                        ErrorHandler.handleMixinError(
                            response.errorCode,
                            response.errorDescription,
                            getString(R.string.setting_desktop_logout_failed)
                        )
                    }
                }
            } else {
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
        if (loggedIn) {
            binding.authTv.text = getString(R.string.setting_logout_desktop)
            binding.descTv.text = getString(R.string.setting_desktop_signed)
            binding.authTv.textColor = requireContext().colorFromAttribute(R.attr.text_blue)
        } else {
            binding.authTv.text = getString(R.string.setting_scan_qr_code)
            binding.descTv.text = getString(R.string.setting_scan_qr_code)
        }
    }

    private val loadOuting: Dialog by lazy {
        indeterminateProgressDialog(
            message = R.string.pb_dialog_message,
            title = R.string.Logout
        ).apply {
            setCancelable(false)
        }
    }

    private fun confirm(url: String) {
        ConfirmBottomFragment.show(requireContext(), childFragmentManager, url) {
            updateUI(true)
        }
    }
}
