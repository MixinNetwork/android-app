package one.mixin.android.ui.conversation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.ActivityConversationPlaceholderBinding
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class ConversationPlaceholderActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityConversationPlaceholderBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}
