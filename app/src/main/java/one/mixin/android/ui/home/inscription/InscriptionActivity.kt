package one.mixin.android.ui.home.inscription

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.uber.autodispose.autoDispose
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.HelpLink.INSCRIPTION
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.databinding.ActivityInscriptionBinding
import one.mixin.android.databinding.ViewInscriptionMenuBinding
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getCapturedImage
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.extension.getPublicDownloadPath
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncInscriptionsJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment.Companion.MAX_PHOTO_SIZE
import one.mixin.android.ui.home.inscription.InscriptionSendActivity.Companion.ARGS_RESULT
import one.mixin.android.ui.home.inscription.component.InscriptionPage
import one.mixin.android.ui.home.inscription.component.SharePage
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.components.InscriptionState
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.Account
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
    private var inScreenshot by mutableStateOf(false)

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
        window.statusBarColor = Color.TRANSPARENT
        binding = ActivityInscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val qrcode = "$INSCRIPTION$inscriptionHash".generateQRCode(dpToPx(110f), 0).first
        binding.compose.setContent {
            InscriptionPage(inscriptionHash, { finish() }, { url, contentType ->
                showBottom(url, contentType)
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
            AnimatedVisibility(visible = isShareDialogVisible && value != null) {
                SharePage(qrcode = qrcode, inscriptionHash = inscriptionHash, value = value!!, inScreenshot = inScreenshot, onClose = {
                    isShareDialogVisible = false
                }, onCopy = onCopy, onSave = onSave, onShare = onShare)
            }
        }

        jobManager.addJobInBackground(SyncInscriptionsJob(listOf(inscriptionHash)))
    }

    private val onSave: (size: IntSize, bottomSize: IntSize) -> Unit = { size, bottomSize ->
        lifecycleScope.launch {
            inScreenshot = true
            delay(100)
            val bitmap = binding.overflow.drawToBitmap()
            val dir = getPublicDownloadPath()
            dir.mkdirs()
            val file = File(dir, "$inscriptionHash.png")
            saveBitmapToFile(file, cropCenterBitmap(bitmap, size.width, size.height, bottomSize.height))
            isShareDialogVisible = false
            MediaScannerConnection.scanFile(this@InscriptionActivity, arrayOf(file.toString()), null, null)
            toast(getString(R.string.Save_to, dir.path))
            inScreenshot = false
        }
    }

    private val onShare: (size: IntSize, bottomSize: IntSize) -> Unit = { size, bottomSize ->
        lifecycleScope.launch {
            inScreenshot = true
            delay(100)
            val bitmap = binding.overflow.drawToBitmap()
            val file = File(cacheDir, "$inscriptionHash.png")
            saveBitmapToFile(file, cropCenterBitmap(bitmap, size.width, size.height, bottomSize.height))
            isShareDialogVisible = false
            val uri = FileProvider.getUriForFile(this@InscriptionActivity, BuildConfig.APPLICATION_ID + ".provider", file)
            val share = Intent()
            share.action = Intent.ACTION_SEND
            share.type = "image/png"
            share.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(share, getString(R.string.Share)))
            inScreenshot = false
        }
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
    private fun showBottom(url:String?, contentType: String?) {
        val builder = BottomSheet.Builder(this)
        _bottomBinding = ViewInscriptionMenuBinding.bind(View.inflate(ContextThemeWrapper(this, R.style.Custom), R.layout.view_inscription_menu, null))
        builder.setCustomView(bottomBinding.root)
        val bottomSheet = builder.create()
        bottomBinding.setAvatarTv.isVisible = contentType?.startsWith("image", true) == true
        bottomBinding.setAvatarTv.setOnClickListener {
            lifecycleScope.launch {
                setAvatar(url)
                bottomSheet.dismiss()
            }
        }
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

    private val imageUri: Uri by lazy {
        Uri.fromFile(this.getOtherPath().createImageTemp())
    }

    private suspend fun setAvatar(url:String?){
        if (url == null) {
            toast(R.string.Please_wait_a_bit)
            return
        }
        val request =
            ImageRequest.Builder(applicationContext)
                .data(url)
                .allowHardware(false) // Disable hardware bitmaps since we're getting a Bitmap
                .build()

        val result = applicationContext.imageLoader.execute(request)
        if (result !is SuccessResult) {
            toast(R.string.Try_Again)
            return
        }
        val f = applicationContext.imageLoader.diskCache?.openSnapshot(url)?.data?.toFile()
        if (f == null) {
            toast(R.string.Try_Again)
            return
        }
        val options = UCrop.Options()
        options.setToolbarColor(ContextCompat.getColor(this@InscriptionActivity, R.color.black))
        options.setStatusBarColor(ContextCompat.getColor(this@InscriptionActivity, R.color.black))
        options.setToolbarWidgetColor(Color.WHITE)
        options.setHideBottomControls(true)
        UCrop.of(f.toUri(), imageUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(
                MAX_PHOTO_SIZE,
                MAX_PHOTO_SIZE,
            )
            .start(this, UCrop.REQUEST_CROP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null) {
                val resultUri = UCrop.getOutput(data)
                val bitmap = resultUri?.getCapturedImage(this.contentResolver)
                update(Base64.encodeToString(bitmap?.toBytes(), Base64.NO_WRAP))
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val cropError = UCrop.getError(data)
                toast(cropError.toString())
            }
        }
    }

    private fun update(
        content: String,
    ) {
        val accountUpdateRequest = AccountUpdateRequest(null, content)
        web3ViewModel.update(accountUpdateRequest)
            .autoDispose(stopScope).subscribe(
                { r: MixinResponse<Account> ->
                    if (!r.isSuccess) {
                        ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                        return@subscribe
                    } else {
                        toast(R.string.Success)
                    }
                    r.data?.let { data ->
                        Session.storeAccount(data)
                        val u = data.toUser()
                        RxBus.publish(u)
                        web3ViewModel.insertUser(u)
                    }
                },
                { t: Throwable ->
                    ErrorHandler.handleError(t)
                },
            )
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
