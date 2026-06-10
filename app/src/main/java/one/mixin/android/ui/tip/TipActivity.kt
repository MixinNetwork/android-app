package one.mixin.android.ui.tip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.DEVICE_ID
import one.mixin.android.R
import one.mixin.android.databinding.ActivityContactBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.tip.TipFragment.Companion.ARGS_SHOULD_WATCH
import one.mixin.android.ui.tip.TipFragment.Companion.ARGS_TIP_BUNDLE
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class TipActivity : BaseActivity() {
    companion object {
        const val TAG = "TipActivity"

        fun show(
            context: Context,
            tipBundle: TipBundle,
            shouldWatch: Boolean = false,
        ) {
            context.startActivity(
                Intent(context, TipActivity::class.java).apply {
                    putExtra(ARGS_TIP_BUNDLE, tipBundle)
                    putExtra(ARGS_SHOULD_WATCH, shouldWatch)
                    setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
            )
        }

        fun show(
            context: Activity,
            tipType: TipType,
            bottomUpAnim: Boolean = false,
            shouldWatch: Boolean = false,
        ) {
            val deviceId = requireNotNull(context.defaultSharedPreferences.getString(DEVICE_ID, null)) { "required deviceId can not be null" }
            val tipBundle = TipBundle(tipType, deviceId, TryConnecting)
            show(context, tipBundle, shouldWatch)
            if (bottomUpAnim) {
                context.overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
            }
        }
    }

    private val binding by viewBinding(ActivityContactBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val tipBundle = intent.getTipBundle()
        val shouldWatch = intent.getBooleanExtra(ARGS_SHOULD_WATCH, false)
        val tipFragment = TipFragment.newInstance(tipBundle, shouldWatch)
        replaceFragment(tipFragment, R.id.container, TipFragment.TAG)
    }
}
