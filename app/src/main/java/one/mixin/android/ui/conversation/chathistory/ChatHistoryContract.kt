package one.mixin.android.ui.conversation.chathistory

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import one.mixin.android.ui.conversation.chathistory.ChatHistoryActivity.Companion.getPinIntent

class ChatHistoryContract : ActivityResultContract<Triple<String, Boolean, Int>, Intent?>() {
    override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
        return intent
    }

    override fun createIntent(context: Context, input: Triple<String, Boolean, Int>): Intent {
        return getPinIntent(context, input.first, input.second, input.third)
    }
}
