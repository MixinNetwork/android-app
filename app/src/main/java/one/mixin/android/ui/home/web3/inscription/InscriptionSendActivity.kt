package one.mixin.android.ui.home.web3.inscription

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.conversation.FriendsFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.web.WebFragment
import one.mixin.android.util.ShareHelper
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.User

@AndroidEntryPoint
class InscriptionSendActivity : BlazeBaseActivity() {
    companion object {
        const val ARGS_RESULT = "args_result"
    }

    class SendContract : ActivityResultContract<String, Intent?>() {
        override fun createIntent(context: Context, input: String): Intent {
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
