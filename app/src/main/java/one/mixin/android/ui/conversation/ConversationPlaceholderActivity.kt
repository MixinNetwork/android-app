package one.mixin.android.ui.conversation

import android.os.Bundle
import android.widget.TextView
import one.mixin.android.R
import one.mixin.android.ui.common.BaseActivity

class ConversationPlaceholderActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_placeholder)
        render()
    }

    override fun onStart() {
        super.onStart()
        ConversationEmbedding.onHomeSectionChanged = { render() }
        render()
    }

    override fun onStop() {
        ConversationEmbedding.onHomeSectionChanged = null
        super.onStop()
    }

    private fun render() {
        findViewById<TextView>(R.id.placeholder_text)?.setText(ConversationEmbedding.homeSection.placeholderRes)
    }
}
