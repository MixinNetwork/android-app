package one.mixin.android.ui.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.ui.common.BaseActivity

@AndroidEntryPoint
class CallActivity : BaseActivity() {

    override fun getDefaultThemeId(): Int {
        return R.style.AppTheme_Call
    }

    override fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_Call
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CallBottomSheetDialogFragment.newInstance(intent.getBooleanExtra(EXTRA_JOIN, false))
            .show(supportFragmentManager, CallBottomSheetDialogFragment.TAG)
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val TAG = "CallActivity"
        const val EXTRA_JOIN = "extra_join"
        const val CHANNEL_PIP_PERMISSION = "channel_pip_permission"
        const val ID_PIP_PERMISSION = 313389

        fun show(context: Context, join: Boolean = false) {
            Intent(context, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_JOIN, join)
            }.run {
                context.startActivity(this)
            }
        }
    }
}
