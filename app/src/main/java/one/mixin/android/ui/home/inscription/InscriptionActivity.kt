package one.mixin.android.ui.home.inscription

import ShareCard
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.HelpLink.INSCRIPTION
import one.mixin.android.R
import one.mixin.android.databinding.ActivityInscriptionBinding
import one.mixin.android.databinding.ViewInscriptionMenuBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.extension.getPublicDownloadPath
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncInscriptionsJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.home.inscription.InscriptionSendActivity.Companion.ARGS_RESULT
import one.mixin.android.ui.home.inscription.component.InscriptionPage
import one.mixin.android.ui.home.inscription.component.ShareBottom
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.components.InscriptionState
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import one.mixin.android.widget.BottomSheet
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class InscriptionActivity : BaseActivity() {
    companion object {
        private const val ARGS_HASH = "args_hash"

        fun show(
            context: Context,
            inscriptionHash: String,
        ) {
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
        requireNotNull(intent.getStringExtra(ARGS_HASH))
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun getDefaultThemeId(): Int {
        return R.style.AppTheme_Transparent
    }

    override fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_Transparent
    }

    private lateinit var binding: ActivityInscriptionBinding
    private var isShareDialogVisible by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSystemUi = true
        super.onCreate(savedInstanceState)
        getSendResult =
            registerForActivityResult(
                InscriptionSendActivity.SendContract(),
                activityResultRegistry,
                ::callbackSend,
            )
        SystemUIManager.lightUI(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        binding = ActivityInscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val qrcode = "$INSCRIPTION$inscriptionHash".generateQRCode(dpToPx(110f), 0).first
        binding.compose.setContent {
            InscriptionPage(inscriptionHash, { finish() }, {
                showBottom()
            }, onSendAction, { isShareDialogVisible = true })
        }
        binding.overflow.setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            val viewModel = hiltViewModel<Web3ViewModel>()
            val liveData = viewModel.inscriptionStateByHash(inscriptionHash)
            val inscription =
                remember {
                    mutableStateOf<InscriptionState?>(null)
                }
            DisposableEffect(inscriptionHash, lifecycleOwner) {
                val observer =
                    Observer<InscriptionState?> {
                        inscription.value = it
                    }
                liveData.observe(lifecycleOwner, observer)
                onDispose { liveData.removeObserver(observer) }
            }
            val value = inscription.value
            val targetSize = remember { mutableStateOf(IntSize.Zero) }

            if (isShareDialogVisible && value != null) {
                Box(
                    modifier = Modifier
                        .background(Color(0xB3000000))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            isShareDialogVisible = false
                        }
                ) {
                    BackHandler(isShareDialogVisible) {
                        isShareDialogVisible = false
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(20.dp)
                            .align(Alignment.Center)
                    ) {
                        ShareCard(modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .wrapContentHeight()
                            .onGloballyPositioned { coordinates ->
                                targetSize.value = coordinates.size
                            }, qrcode = qrcode, inscriptionHash, value
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ShareBottom(modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(16.dp), onShare = { bottomSize ->
                            onShare(targetSize.value, bottomSize)
                        }, onCopy = onCopy, onSave = { bottomSize ->
                            onSave(targetSize.value, bottomSize)
                        })
                    }
                }
            }
        }

        jobManager.addJobInBackground(SyncInscriptionsJob(listOf(inscriptionHash)))
    }

    val onSave: (size: IntSize, bottomSize: IntSize) -> Unit = { size, bottomSize ->
        isShareDialogVisible = false
        val bitmap = binding.overflow.drawToBitmap()
        val dir = getPublicDownloadPath()
        dir.mkdirs()
        val file = File(dir, "$inscriptionHash.png")
        saveBitmapToFile(file, cropCenterBitmap(bitmap, size.width, size.height, bottomSize.height))
        toast(R.string.Save_success)
    }

    val onShare: (size: IntSize, bottomSize: IntSize) -> Unit = { size, bottomSize ->
        isShareDialogVisible = false
        val bitmap = binding.overflow.drawToBitmap()
        val file = File(cacheDir, "$inscriptionHash.png")
        saveBitmapToFile(file, cropCenterBitmap(bitmap, size.width, size.height, bottomSize.height))

        val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)

        val share = Intent()
        share.action = Intent.ACTION_SEND
        share.type = "image/png"
        share.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(share, getString(R.string.Share)))
    }

    private val onCopy: () -> Unit = {
        isShareDialogVisible = false
        getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, "$INSCRIPTION$inscriptionHash"))
        toast(R.string.copied_to_clipboard)
    }

    private fun cropCenterBitmap(sourceBitmap: Bitmap, targetWidth: Int, targetHeight: Int, bottomHeight: Int): Bitmap {
        val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val startX = (sourceBitmap.width - targetWidth) / 2
        val startY = (sourceBitmap.height - targetHeight - bottomHeight) / 2 - dpToPx(22f)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val path = Path()
        val rectF = RectF(0f, 0f, resultBitmap.width.toFloat(), resultBitmap.height.toFloat())
        val cornerRadius = dpToPx(8f).toFloat()
        path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(path)
        canvas.drawBitmap(sourceBitmap, -startX.toFloat(), -startY.toFloat(), paint)
        return resultBitmap
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

    private var _bottomBinding: ViewInscriptionMenuBinding? = null
    private val bottomBinding get() = requireNotNull(_bottomBinding)

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(this)
        _bottomBinding = ViewInscriptionMenuBinding.bind(View.inflate(ContextThemeWrapper(this, R.style.Custom), R.layout.view_inscription_menu, null))
        builder.setCustomView(bottomBinding.root)
        val bottomSheet = builder.create()
        bottomBinding.cancelTv.setOnClickListener {
            bottomSheet.dismiss()
        }
        bottomBinding.releaseTv.setOnClickListener {
            lifecycleScope.launch {
                val self = Session.getAccount()?.toUser() ?: return@launch
                val nftBiometricItem = web3ViewModel.buildNftTransaction(inscriptionHash, self, true) ?: return@launch
                TransferBottomSheetDialogFragment.newInstance(nftBiometricItem).show(supportFragmentManager, TransferBottomSheetDialogFragment.TAG)
            }

            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
