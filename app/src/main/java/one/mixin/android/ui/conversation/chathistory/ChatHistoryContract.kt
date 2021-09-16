package one.mixin.android.ui.conversation.chathistory

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import one.mixin.android.ui.conversation.chathistory.ChatHistoryActivity.Companion.getPinIntent

class ChatHistoryContract : ActivityResultContract<Pair<String, Boolean>, Intent?>() {
    override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
        return intent
    }

    override fun createIntent(context: Context, input: Pair<String, Boolean>): Intent {
        return getPinIntent(context, input.first, input.second)
    }
}
