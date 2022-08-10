package one.mixin.android.ui.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.databinding.ActivityContactBinding
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class SharedMediaActivity : BaseActivity() {
    companion object {
        fun show(context: Context, conversationId: String) {
            Intent(context, SharedMediaActivity::class.java).run {
                putExtra(ARGS_CONVERSATION_ID, conversationId)
                context.startActivity(this)
            }
        }
    }

    class SharedMediaContract : ActivityResultContract<String, Intent?>() {
        override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
            return intent
        }

        override fun createIntent(context: Context, input: String): Intent {
            return Intent(context, SharedMediaActivity::class.java).apply {
                putExtra(ARGS_CONVERSATION_ID, input)
                context.startActivity(this)
            }
        }
    }

    private val binding by viewBinding(ActivityContactBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val conversationId = intent.getStringExtra(ARGS_CONVERSATION_ID)
        require(conversationId != null)
        replaceFragment(SharedMediaFragment.newInstance(conversationId), R.id.container, SharedMediaFragment.TAG)
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioPlayer.pause()
        AudioPlayer.release()
    }
}
