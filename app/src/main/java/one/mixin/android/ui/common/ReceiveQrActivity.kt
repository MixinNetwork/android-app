package one.mixin.android.ui.common

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.drawToBitmap
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.android.lifecycle.autoDispose
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.InputAmountBottomSheetDialogFragment
import one.mixin.android.databinding.ActivityReceiveQrBinding
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.isExternalTransferUrl
import one.mixin.android.extension.isLightningUrl
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.home.MainActivity.Companion.SCAN
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_SHOW_SCAN
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment.Companion.TYPE_FROM_RECEIVE
import one.mixin.android.ui.wallet.BackupMnemonicPhraseWarningBottomSheetDialogFragment
import one.mixin.android.ui.wallet.DepositShareActivity
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.widget.BadgeCircleImageView
import timber.log.Timber

@AndroidEntryPoint
class ReceiveQrActivity : BaseActivity() {

    companion object {
        const val ARGS_USER_ID = "args_user_id"

        fun show(context: android.content.Context, userId: String) {
            val intent = android.content.Intent(context, ReceiveQrActivity::class.java)
            intent.putExtra(ARGS_USER_ID, userId)
            context.startActivity(intent)
        }
    }

    private val binding by lazy { ActivityReceiveQrBinding.inflate(layoutInflater) }
    private val viewModel: BottomSheetViewModel by viewModels()

    private val userId: String by lazy { intent.getStringExtra(ARGS_USER_ID)!! }

    private val getScanResult =
        registerForActivityResult(CaptureActivity.CaptureContract()) { data ->
            if (data != null) {
                handlerCode(data)
            }
        }

    fun showCapture(scan: Boolean) {
        getScanResult.launch(Pair(ARGS_SHOW_SCAN, scan))
    }

    private var bottomSheet: DialogFragment? = null
    private fun handlerCode(intent: Intent) {
        if (intent.hasExtra(SCAN)) {
            val scan = intent.getStringExtra(SCAN)!!
            bottomSheet?.dismiss()
            showScanBottom(scan)
            clearCodeAfterConsume(intent, SCAN)
        }
    }

    private fun showScanBottom(scan: String) {
        if (scan.isLightningUrl() || scan.isExternalTransferUrl()) {
            LinkBottomSheetDialogFragment.newInstance(scan).show(
                supportFragmentManager,
                LinkBottomSheetDialogFragment.TAG
            )
        } else {
            bottomSheet = QrScanBottomSheetDialogFragment.newInstance(scan)
            bottomSheet?.showNow(supportFragmentManager, QrScanBottomSheetDialogFragment.TAG)
        }
    }

    private fun clearCodeAfterConsume(
        intent: Intent,
        code: String,
    ) {
        intent.removeExtra(code)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.titleView.setSubTitle(getString(R.string.Receive_Money), getString(R.string.Privacy_Wallet), R.drawable.ic_wallet_privacy)
        binding.titleView.leftIb.setOnClickListener {
            finish()
        }
        viewModel.findUserById(userId).observe(
            this,
        ) { user ->
            if (user == null) {
                viewModel.refreshUser(userId, true)
            } else {
                binding.apply {
                    scan.setOnClickListener {
                        RxPermissions(this@ReceiveQrActivity).request(Manifest.permission.CAMERA)
                            .autoDispose(stopScope).subscribe { granted ->
                                if (granted) {
                                    showCapture(true)
                                } else {
                                    this@ReceiveQrActivity.openPermissionSetting()
                                }
                            }
                    }
                    amount.setOnClickListener {
                        if (!Session.saltExported() && Session.isAnonymous()) {
                            BackupMnemonicPhraseWarningBottomSheetDialogFragment.newInstance()
                                .apply {
                                    laterCallback = {
                                        showReceiveAssetList()
                                    }
                                }
                                .show(supportFragmentManager, BackupMnemonicPhraseWarningBottomSheetDialogFragment.TAG)
                        } else {
                            showReceiveAssetList()
                        }
                    }
                    share.setOnClickListener {
                        val shareView = binding.container
                        val bitmap = shareView.drawToBitmap()
                        DepositShareActivity.show(
                            this@ReceiveQrActivity,
                            bitmap,
                            null,
                            "${Constants.Scheme.HTTPS_PAY}/${Session.getSessionId()}",
                        )
                    }
                    badgeView.bg.setInfo(user.fullName, user.avatarUrl, user.userId)
                    nameTv.text = user.fullName
                    numberTv.text = getString(R.string.contact_mixin_id, user.identityNumber)
                    badgeView.badge.setImageResource(R.drawable.ic_contacts_receive_blue)
                    badgeView.pos = BadgeCircleImageView.END_BOTTOM
                    qr.post {
                        Observable.create<Pair<Bitmap, Int>> { e ->
                            val code = "${Constants.Scheme.HTTPS_PAY}/${user.userId}"
                            val r = code.generateQRCode(binding.qr.width)
                            e.onNext(r)
                        }.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .autoDispose(this@ReceiveQrActivity)
                            .subscribe(
                                { r ->
                                    badgeView.layoutParams =
                                        badgeView.layoutParams.apply {
                                            width = r.second
                                            height = r.second
                                        }
                                    qr.setImageBitmap(r.first)
                                },
                                {
                                },
                            )
                    }
                }
            }
        }
    }

    private fun showReceiveAssetList() {
        AssetListBottomSheetDialogFragment.newInstance(TYPE_FROM_RECEIVE)
            .setOnAssetClick { asset ->
                InputAmountBottomSheetDialogFragment.newInstance(
                    asset,
                    "${Constants.Scheme.HTTPS_PAY}/${Session.getSessionId()}"
                ).apply {
                    this.onCopyClick = { address ->
                        this@ReceiveQrActivity.lifecycleScope.launch {
                            context?.heavyClickVibrate()
                            context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, address))
                            toast(R.string.copied_to_clipboard)
                        }
                    }
                    this.onShareClick = { amount, address ->
                        this@ReceiveQrActivity.lifecycleScope.launch {
                            val shareView = binding.container
                            val bitmap = shareView.drawToBitmap()
                            DepositShareActivity.show(
                                requireContext(),
                                bitmap,
                                asset,
                                "${Constants.Scheme.HTTPS_PAY}/${Session.getSessionId()}",
                                        address,
                                amount
                            )
                        }
                    }
                }.showNow(supportFragmentManager, InputAmountBottomSheetDialogFragment.TAG)
            }.showNow(supportFragmentManager, AssetListBottomSheetDialogFragment.TAG)
    }

}
