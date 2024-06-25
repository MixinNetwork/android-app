package one.mixin.android.ui.home.inscription

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.conversation.FriendsFragment

@AndroidEntryPoint
class InscriptionSendActivity : BlazeBaseActivity() {
    companion object {
        const val ARGS_RESULT = "args_result"
    }

    class SendContract : ActivityResultContract<String, Intent?>() {
        override fun createIntent(
            context: Context,
            input: String,
        ): Intent {
            return Intent(context, InscriptionSendActivity::class.java).apply {
            }
        }

        override fun parseResult(
            resultCode: Int,
            intent: Intent?,
        ): Intent? {
            if (intent == null || resultCode != Activity.RESULT_OK) return null
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        supportFragmentManager.beginTransaction().add(
            R.id.container,
            FriendsFragment.newInstance(true).apply {
                setOnFriendClick {
                    val result = Intent().apply { putExtra(ARGS_RESULT, it) }
                    setResult(Activity.RESULT_OK, result)
                    finish()
                }
            },
            FriendsFragment.TAG,
        ).commit()
    }
}
