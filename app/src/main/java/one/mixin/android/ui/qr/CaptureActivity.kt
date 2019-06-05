package one.mixin.android.ui.qr

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BlazeBaseActivity

class CaptureActivity : BlazeBaseActivity() {

    private lateinit var captureFragment: BaseCaptureFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        val isGooglePlayServicesAvailable = isGooglePlayServicesAvailable()
        captureFragment = if (isGooglePlayServicesAvailable) {
            when {
                intent.hasExtra(ARGS_FOR_ADDRESS) -> CameraXCaptureFragment.newInstance(true)
                intent.hasExtra(ARGS_FOR_ACCOUNT_NAME) -> CameraXCaptureFragment.newInstance(forAccountName = true)
                else -> CameraXCaptureFragment.newInstance()
            }
        } else {
            when {
                intent.hasExtra(ARGS_FOR_ADDRESS) -> ZxingCaptureFragment.newInstance(true)
                intent.hasExtra(ARGS_FOR_ACCOUNT_NAME) -> ZxingCaptureFragment.newInstance(forAccountName = true)
                else -> ZxingCaptureFragment.newInstance()
            }
        }
        val tag = if (isGooglePlayServicesAvailable) CameraXCaptureFragment.TAG else ZxingCaptureFragment.TAG
        replaceFragment(captureFragment, R.id.container, tag)
    }

    override fun onResume() {
        super.onResume()
        resumeCapture()
    }

    fun resumeCapture() {
        if (!isGooglePlayServicesAvailable()) {
            (captureFragment as? ZxingCaptureFragment)?.resume()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }

    companion object {
        const val SHOW_QR_CODE = "show_qr_code"

        const val ARGS_FOR_ADDRESS = "args_for_address"
        const val ARGS_ADDRESS_RESULT = "args_address_result"
        const val ARGS_FOR_ACCOUNT_NAME = "args_for_account_name"
        const val ARGS_ACCOUNT_NAME_RESULT = "args_account_name_result"

        const val REQUEST_CODE = 0x0000c0ff
        const val RESULT_CODE = 0x0000c0df

        val SCOPES = arrayListOf("PROFILE:READ", "PHONE:READ", "ASSETS:READ", "APPS:READ", "APPS:WRITE", "CONTACTS:READ")

        const val MAX_DURATION = 15
        const val MIN_DURATION = 1

        fun show(
            activity: Activity,
            actionWithIntent: ((intent: Intent) -> Unit)? = null
        ) {
            Intent(activity, CaptureActivity::class.java).apply {
                activity.overridePendingTransition(R.anim.slide_in_bottom, 0)
                if (actionWithIntent == null) {
                    activity.startActivity(this)
                } else {
                    actionWithIntent.invoke(this)
                }
            }
        }
    }
}
