package one.mixin.android.ui.qr

import android.graphics.Bitmap
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.qr.CaptureFragment.Companion.ARGS_FOR_ACCOUNT_NAME
import one.mixin.android.ui.qr.CaptureFragment.Companion.ARGS_FOR_ADDRESS
import one.mixin.android.ui.qr.CaptureFragment.Companion.newInstance

class CaptureActivity : BlazeBaseActivity(), CaptureFragment.Callback, EditFragment.Callback {

    private lateinit var captureFragment: CaptureFragment

    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        captureFragment = when {
            intent.hasExtra(ARGS_FOR_ADDRESS) -> newInstance(true)
            intent.hasExtra(ARGS_FOR_ACCOUNT_NAME) -> newInstance(forAccountName = true)
            else -> newInstance()
        }
        replaceFragment(captureFragment, R.id.container, CaptureFragment.TAG)
    }

    override fun onResume() {
        super.onResume()
        resumeCapture()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }

    override fun getBitmap(): Bitmap? {
        return bitmap
    }

    override fun setBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
    }

    override fun resumeCapture() {
        captureFragment.resume()
    }
}
