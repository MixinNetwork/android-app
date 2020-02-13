package one.mixin.android.ui.qr

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.util.isCameraCanUse

class CaptureActivity : BlazeBaseActivity() {

    private lateinit var captureFragment: BaseCaptureFragment

    override fun getDefaultThemeId(): Int {
        return R.style.AppTheme_Capture
    }

    override fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_Capture
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in_bottom, 0)
        checkCameraCanUse()
        setContentView(R.layout.activity_contact)
        captureFragment = when {
                intent.hasExtra(ARGS_FOR_ADDRESS) -> CameraXCaptureFragment.newInstance(true)
                intent.hasExtra(ARGS_FOR_ACCOUNT_NAME) -> CameraXCaptureFragment.newInstance(forAccountName = true)
                intent.hasExtra(ARGS_FOR_MEMO) -> CameraXCaptureFragment.newInstance(forMemo = true)
                else -> CameraXCaptureFragment.newInstance()
            }
        replaceFragment(captureFragment, R.id.container, CameraXCaptureFragment.TAG)
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

    companion object {
        const val SHOW_QR_CODE = "show_qr_code"

        const val ARGS_FOR_ADDRESS = "args_for_address"
        const val ARGS_ADDRESS_RESULT = "args_address_result"
        const val ARGS_FOR_ACCOUNT_NAME = "args_for_account_name"
        const val ARGS_ACCOUNT_NAME_RESULT = "args_account_name_result"
        const val ARGS_FOR_MEMO = "args_for_memo"
        const val ARGS_MEMO_RESULT = "args_memo_result"

        const val REQUEST_CODE = 0x0000c0ff
        const val RESULT_CODE = 0x0000c0df

        const val MAX_DURATION = 15
        const val MIN_DURATION = 1

        fun show(
            activity: Activity,
            actionWithIntent: ((intent: Intent) -> Unit)? = null
        ) {
            Intent(activity, CaptureActivity::class.java).apply {
                if (actionWithIntent == null) {
                    activity.startActivity(this)
                } else {
                    actionWithIntent.invoke(this)
                }
            }
        }
    }
}
