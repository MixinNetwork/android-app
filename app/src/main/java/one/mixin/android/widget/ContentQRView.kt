package one.mixin.android.widget

import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ViewAnimator
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.uber.autodispose.ScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.R
import one.mixin.android.databinding.ViewContentQrBinding
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.DepositQrBottomFragment
import one.mixin.android.vo.AssetItem

class ContentQRView : ViewAnimator {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        binding = ViewContentQrBinding.inflate(LayoutInflater.from(context), this)
    }

    private val binding: ViewContentQrBinding

    fun setAsset(parentFragmentManager: FragmentManager, scopeProvider: ScopeProvider, asset: AssetItem, isTag: Boolean, warning: String? = null) {
        binding.apply {
            val showPb = if (isTag) {
                asset.getTag().isNullOrBlank()
            } else {
                asset.getDestination().isBlank()
            }

            (binding.root as ViewAnimator).displayedChild = if (showPb) 1 else 0

            if (showPb) return

            qrAvatar.apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
                setBorder()
            }
            val content = if (isTag) asset.getTag() else asset.getDestination()
            contentTv.text = content
            copyIv.setOnClickListener {
                context.heavyClickVibrate()
                context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, content))
                toast(R.string.copied_to_clipboard)
            }
            if (warning.isNullOrBlank()) {
                warningTv.isVisible = false
            } else {
                warningTv.text = warning
                warningTv.isVisible = true
            }
            qrFl.setOnClickListener {
                DepositQrBottomFragment.newInstance(asset, if (isTag) DepositQrBottomFragment.TYPE_TAG else DepositQrBottomFragment.TYPE_ADDRESS)
                    .show(parentFragmentManager, DepositQrBottomFragment.TAG)
            }
            qr.post {
                Observable.create<Pair<Bitmap, Int>> { e ->
                    val r = if (isTag) {
                        requireNotNull(asset.getTag())
                    } else {
                        asset.getDestination()
                    }.generateQRCode(qr.width)
                    e.onNext(r)
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(scopeProvider)
                    .subscribe(
                        { r ->
                            qrAvatar.layoutParams = qrAvatar.layoutParams.apply {
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
