package one.mixin.android.ui.landing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.ActivityLandingBinding
import one.mixin.android.extension.replaceFragment
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.viewBinding
import javax.inject.Inject

@AndroidEntryPoint
class LandingActivity : BaseActivity() {

    companion object {
        const val ARGS_PIN = "args_pin"

        fun show(context: Context) {
            val intent = Intent(context, LandingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        }

        fun show(context: Context, pinCode: String) {
            val intent = Intent(context, LandingActivity::class.java).apply {
                putExtra(ARGS_PIN, pinCode)
            }
            context.startActivity(intent)
        }
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private val binding by viewBinding(ActivityLandingBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val pin = intent.getStringExtra(ARGS_PIN)
        val fragment = if (pin != null) {
            MobileFragment.newInstance(pin)
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                jobManager.clear()
            }
            LandingFragment.newInstance()
        }
        replaceFragment(fragment, R.id.container)
    }
}
