package one.mixin.android.ui.qr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContract
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.conversation.ConversationActivity.Companion.ARGS_SHORTCUT
import one.mixin.android.util.isCameraCanUse
import one.mixin.android.util.rxpermission.RxPermissions

@AndroidEntryPoint
class CaptureActivity : BlazeBaseActivity() {
    override fun getDefaultThemeId(): Int {
        return R.style.AppTheme_Capture
    }

    override fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_Capture
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        overridePendingTransition(R.anim.slide_in_bottom, 0)
        checkCameraCanUse()
        setContentView(R.layout.activity_contact)

        RxPermissions(this)
            .request(Manifest.permission.CAMERA)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    initView()
                } else {
                    openPermissionSetting()
                }
            }
    }

    private fun initView() {
        when {
            intent.hasExtra(ARGS_FOR_SCAN_RESULT) ->
                replaceFragment(ScanFragment.newInstance(forScanResult = true), R.id.container, ScanFragment.TAG)
            else ->
                if (intent.getBooleanExtra(ARGS_SHOW_SCAN, false)) {
                    val fromShortcut = intent.getBooleanExtra(ARGS_SHORTCUT, false)
                    replaceFragment(ScanFragment.newInstance(fromShortcut = fromShortcut), R.id.container, ScanFragment.TAG)
                } else {
                    replaceFragment(CaptureFragment.newInstance(), R.id.container, CaptureFragment.TAG)
                }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }

    private fun checkCameraCanUse() {
        if (Build.MANUFACTURER == "smartisan" || Build.MANUFACTURER == "Meizu" || Build.MANUFACTURER == "YOTA") {
            if (!isCameraCanUse()) {
                toast(R.string.need_camera_permission)
                finish()
            }
        }
    }

    class CaptureContract : ActivityResultContract<Pair<String, Boolean>, Intent?>() {
        override fun createIntent(
            context: Context,
            input: Pair<String, Boolean>,
        ): Intent {
            return Intent(context, CaptureActivity::class.java).apply {
                putExtra(input.first, input.second)
            }
        }

        override fun parseResult(
            resultCode: Int,
            intent: Intent?,
        ): Intent? {
            if (intent == null || resultCode != Activity.RESULT_OK) return null
            return intent
        }
    }

    companion object {
        const val SHOW_QR_CODE = "show_qr_code"

        const val ARGS_SHOW_SCAN = "args_show_scan"
        const val ARGS_FOR_SCAN_RESULT = "args_for_scan_result"

        const val MAX_DURATION = 30
        const val MIN_DURATION = 1
    }
}
