package one.mixin.android.widget

import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ViewAnimator
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import com.uber.autodispose.ScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.ViewContentQrBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.dp
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.DepositQrBottomFragment
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.TokenItem

class ContentQRView : ViewAnimator {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        binding = ViewContentQrBinding.inflate(LayoutInflater.from(context), this)
        binding.qrAvatar.setBadgeBorder()
    }

    private val binding: ViewContentQrBinding

    fun setAsset(
        parentFragmentManager: FragmentManager,
        scopeProvider: ScopeProvider,
        asset: TokenItem,
        depositEntry: DepositEntry,
        selectedDestination: String?,
        isTag: Boolean,
        warning: String? = null,
        hideCopy: Boolean = false,
    ) {
        binding.apply {
            val showPb =
                if (isTag) {
                    depositEntry.tag.isNullOrBlank()
                } else {
                    depositEntry.destination.isBlank()
                }
            (binding.root as ViewAnimator).displayedChild = if (showPb) 1 else 0

            if (showPb) return

            qrAvatar.apply {
                loadToken(asset)
                setBorder()
            }
            val destination = selectedDestination ?: depositEntry.destination
            val content = if (isTag) depositEntry.tag else destination
            contentTv.text = content
            if (hideCopy) {
                copyIv.isVisible = false
                contentTv.updateLayoutParams<MarginLayoutParams> {
                    marginEnd = 16.dp
                    marginStart = 16.dp
                }
            } else {
                copyIv.isVisible = true
                copyIv.setOnClickListener {
                    context.heavyClickVibrate()
                    context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, content))
                    toast(R.string.copied_to_clipboard)
                }
            }
            if (warning.isNullOrBlank()) {
                warningTv.isVisible = false
            } else {
                warningTv.text = warning
                warningTv.isVisible = true
            }
            qrFl.setOnClickListener {
                DepositQrBottomFragment.newInstance(asset, depositEntry, if (isTag) DepositQrBottomFragment.TYPE_TAG else DepositQrBottomFragment.TYPE_ADDRESS, selectedDestination)
                    .show(parentFragmentManager, DepositQrBottomFragment.TAG)
            }
            qr.doOnPreDraw {
                Observable.create<Pair<Bitmap, Int>> { e ->
                    val r =
                        if (isTag) {
                            requireNotNull(depositEntry.tag)
                        } else {
                            if (asset.chainId == Constants.ChainId.LIGHTNING_NETWORK_CHAIN_ID) {
                                destination.uppercase()
                            } else {
                                destination
                            }
                        }.generateQRCode(220.dp, innerPadding = 40.dp, padding = 16.dp)
                    e.onNext(r)
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(scopeProvider)
                    .subscribe(
                        { r ->
                            qr.setImageBitmap(r.first)
                        },
                        {
                        },
                    )
            }
        }
    }

    fun loadAddress(
        scopeProvider: ScopeProvider,
        destination: String,
        web3Token: Web3TokenItem,
        warning: String? = null,
    ) {
        binding.apply {
            (binding.root as ViewAnimator).displayedChild = 0

            qrAvatar.apply {
                bg.loadImage(web3Token.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(web3Token.chainIcon, R.drawable.ic_avatar_place_holder)
                setBorder()
            }
            contentTv.text = destination
            copyIv.isVisible = false
            contentTv.updateLayoutParams<MarginLayoutParams> {
                marginEnd = 16.dp
                marginStart = 16.dp
            }
            if (warning.isNullOrBlank()) {
                warningTv.isVisible = false
            } else {
                warningTv.text = warning
                warningTv.isVisible = true
            }

            qr.doOnPreDraw {
                Observable.create<Pair<Bitmap, Int>> { e ->
                    val r = destination.generateQRCode(220.dp, innerPadding = 40.dp, padding = 16.dp)
                    e.onNext(r)
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(scopeProvider)
                    .subscribe(
                        { r ->
                            qr.setImageBitmap(r.first)
                        },
                        {
                        },
                    )
            }
        }
    }

}
