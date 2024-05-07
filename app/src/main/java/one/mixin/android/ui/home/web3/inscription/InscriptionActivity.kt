package one.mixin.android.ui.home.web3.inscription

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.extension.toast
import one.mixin.android.ui.home.web3.components.InscriptionPage
import one.mixin.android.ui.home.web3.inscription.InscriptionSendActivity.Companion.ARGS_RESULT
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.User

@AndroidEntryPoint
class InscriptionActivity : AppCompatActivity() {
    companion object {
        private const val ARGS_HASH = "args_hash"
        fun show(context: Context, inscriptionHash:String) {
            Intent(context, InscriptionActivity::class.java).apply {
                putExtra(ARGS_HASH, inscriptionHash)
            }.run {
                context.startActivity(this)
            }
        }
    }

    private lateinit var getSendResult: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getSendResult =
            registerForActivityResult(
                InscriptionSendActivity.SendContract(),
                activityResultRegistry,
                ::callbackSend,
            )
        SystemUIManager.lightUI(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        val inscriptionHash = requireNotNull( intent.getStringExtra(ARGS_HASH))
        setContent {
            InscriptionPage(inscriptionHash, onSendAction, onShareAction)
        }
    }

    private val onSendAction = {
        getSendResult.launch("")
    }

    private fun callbackSend(data: Intent?) {
        val user = data?.getParcelableExtraCompat(ARGS_RESULT,User::class.java)?:return
        toast(user.userId)
        // todo
        // TransferBottomSheetDialogFragment.newInstance(NftBiometricItem())

    }

    private val onShareAction = {
        // Todo
        toast("Coming soon")
        Unit
    }
}




