package one.mixin.android.ui.home.inscription

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.HelpLink.INSCRIPTION
import one.mixin.android.R
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncInscriptionsJob
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.inscription.component.InscriptionPage
import one.mixin.android.ui.home.inscription.InscriptionSendActivity.Companion.ARGS_RESULT
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.User
import javax.inject.Inject

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

    private val web3ViewModel by viewModels<Web3ViewModel>()

    private val inscriptionHash by lazy {
        requireNotNull( intent.getStringExtra(ARGS_HASH))
    }
    @Inject
    lateinit var jobManager: MixinJobManager

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
        setContent {
            InscriptionPage(inscriptionHash, { finish() }, onSendAction, onShareAction)
        }
        jobManager.addJobInBackground(SyncInscriptionsJob(listOf(inscriptionHash)))
    }

    private val onSendAction = {
        getSendResult.launch("")
    }

    private fun callbackSend(data: Intent?) {
        val user = data?.getParcelableExtraCompat(ARGS_RESULT, User::class.java) ?: return
        lifecycleScope.launch {
            val nftBiometricItem = web3ViewModel.buildNftTransaction(inscriptionHash, user) ?: return@launch
            TransferBottomSheetDialogFragment.newInstance(nftBiometricItem).show(supportFragmentManager, TransferBottomSheetDialogFragment.TAG)
        }
    }

    private val onShareAction = {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(
            Intent.EXTRA_TEXT,
            "$INSCRIPTION$inscriptionHash"
        )
        sendIntent.type = "text/plain"
        startActivity(
            Intent.createChooser(
                sendIntent,
                resources.getText(R.string.Share),
            ),
        )
    }
}




