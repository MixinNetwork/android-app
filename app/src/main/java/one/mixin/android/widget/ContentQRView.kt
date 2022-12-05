package one.mixin.android.widget

import android.content.ClipData
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ViewAnimator
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.databinding.ViewContentQrBinding
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.DepositQrBottomFragment
import one.mixin.android.vo.AssetItem

class ContentQRView : ViewAnimator, LifecycleOwner {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        binding = ViewContentQrBinding.inflate(LayoutInflater.from(context), this)
    }

    private val binding: ViewContentQrBinding

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val lifecycleScope
        get() = lifecycleRegistry.coroutineScope

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } else if (visibility == GONE || visibility == INVISIBLE) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    fun setAsset(
        parentFragmentManager: FragmentManager,
        asset: AssetItem,
        isTag: Boolean,
        warning: String? = null
    ) {
        binding.apply {
            val showPb = if (isTag) {
                asset.getTag().isNullOrBlank()
            } else {
                asset.getDestination().isBlank()
            }

            (binding.root as ViewAnimator).displayedChild = if (showPb) 1 else 0

            if (showPb) return

            val content = if (isTag) asset.getTag() else asset.getDestination()
            contentTv.text = content
            copyIv.setOnClickListener {
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
                DepositQrBottomFragment.newInstance(asset, if (isTag) DepositQrBottomFragment.TYPE_TAG else DepositQrBottomFragment.TYPE_ADDRESS).show(parentFragmentManager, DepositQrBottomFragment.TAG)
            }
            qrAvatar.apply {
                setContent(asset.iconUrl, asset.chainIconUrl, 2)
            }
            doOnPreDraw {
                lifecycleScope.launch {
                    val tag = if (isTag) {
                        requireNotNull(asset.getTag())
                    } else {
                        asset.getDestination()
                    }
                    val result = withContext(Dispatchers.IO) {
                        tag.generateQRCode(qr.width)
                    }
                    qrAvatar.layoutParams = qrAvatar.layoutParams.apply {
                        width = result.second
                        height = result.second
                    }
                    qr.setImageBitmap(result.first)
                }
            }
        }
    }
}
