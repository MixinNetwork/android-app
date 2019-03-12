package one.mixin.android.ui.device

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.google.zxing.integration.android.IntentIntegrator
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_device.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.AvatarActivity.Companion.ARGS_URL
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.textColor

class DeviceFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DeviceFragment"

        fun newInstance(url: String? = null) = DeviceFragment().withArgs {
            if (url != null) {
                putString(ARGS_URL, url)
            }
        }
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        ErrorHandler.handleError(exception)
    }

    private var disposable: Disposable? = null

    private var loggedIn = false

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_device, null)
        contentView.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        (dialog as BottomSheet).apply {
            fullScreen = true
            setCustomView(contentView)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.left_ib.setOnClickListener { dismiss() }
        contentView.auth_tv.setOnClickListener {
            if (loggedIn) {
                loadOuting.show()
                GlobalScope.launch(coroutineExceptionHandler) {
                    val response = bottomViewModel.logoutAsync().await()
                    if (response.isSuccess) {
                        withContext(Dispatchers.Main) {
                            loadOuting.dismiss()
                            updateUI(false)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            loadOuting.dismiss()
                            toast(R.string.setting_desktop_logout_failed)
                        }
                    }
                }
            } else {
                val intentIntegrator = IntentIntegrator(activity)
                intentIntegrator.captureActivity = CaptureActivity::class.java
                intentIntegrator.setBeepEnabled(false)
                val intent = intentIntegrator.createScanIntent().putExtra(CaptureFragment.ARGS_FOR_ADDRESS, true)
                startActivityForResult(intent, IntentIntegrator.REQUEST_CODE)
                activity?.overridePendingTransition(R.anim.slide_in_bottom, 0)
            }
        }
        checkSession()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == CaptureFragment.RESULT_CODE) {
            val url = data?.getStringExtra(CaptureFragment.ARGS_ADDRESS_RESULT)
            url?.let {
                confirm(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

    private fun checkSession() {
        val sessionId = Session.getExtensionSessionId()
        if (sessionId != null) {
            updateUI(true)
        }
    }

    private fun updateUI(loggedIn: Boolean) {
        this.loggedIn = loggedIn
        if (loggedIn) {
            contentView.auth_tv.text = getString(R.string.setting_logout_desktop)
            contentView.desc_tv.text = getString(R.string.setting_desktop_signed)
            contentView.auth_tv.textColor = R.color.colorDarkBlue
            contentView.logo_iv.setImageResource(R.drawable.ic_desktop_online)
        } else {
            contentView.auth_tv.text = getString(R.string.setting_scan_qr_code)
            contentView.desc_tv.text = getString(R.string.setting_scan_qr_code)
            contentView.logo_iv.setImageResource(R.drawable.ic_desktop_offline)
        }
    }

    private val loadOuting: Dialog by lazy {
        indeterminateProgressDialog(message = R.string.pb_dialog_message,
            title = R.string.setting_desktop_logout).apply {
            setCancelable(false)
        }
    }

    private fun confirm(url: String) {
        val confirmBottomFragment = ConfirmBottomFragment.newInstance(url)
        confirmBottomFragment.setCallBack {
            updateUI(true)
        }
        confirmBottomFragment.show(fragmentManager, ConfirmBottomFragment.TAG)
    }
}
