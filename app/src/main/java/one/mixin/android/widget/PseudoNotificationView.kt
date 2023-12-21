package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.widget.RelativeLayout
import androidx.collection.ArraySet
import androidx.core.content.res.ResourcesCompat
import one.mixin.android.R
import one.mixin.android.databinding.ViewPseudoNotificationBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.isExternalTransferUrl
import one.mixin.android.extension.isMixinUrl

class PseudoNotificationView : RelativeLayout {
    var currContent: String? = null

    private val contentSet = ArraySet<String>()
    private var visible = false

    lateinit var callback: Callback

    private val binding = ViewPseudoNotificationBinding.inflate(LayoutInflater.from(context), this, true)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val d = ResourcesCompat.getDrawable(resources, R.drawable.ic_qr_code_preview, context.theme)
        val size = 12.dp
        d?.setBounds(0, 0, size, size)
        binding.titleTv.setCompoundDrawables(d, null, null, null)
    }

    fun addContent(s: String) {
        if (contentSet.contains(s)) {
            return
        }
        contentSet.add(s)
        currContent = s
        binding.contentTv.text =
            if (s.isMixinUrl() || s.isExternalTransferUrl()) {
                context.getString(R.string.detect_qr_tip)
            } else {
                s
            }
        if (!visible) {
            animate().apply {
                translationY(0f)
                interpolator = DecelerateInterpolator()
            }.start()
            visible = true
        }
    }

    private fun hide() {
        animate().apply {
            translationY(-300.dp.toFloat())
            interpolator = DecelerateInterpolator()
        }.start()
        visible = false
    }

    private val gestureDetector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float,
                ): Boolean {
                    hide()
                    return super.onFling(e1, e2, velocityX, velocityY)
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    currContent?.let { callback.onClick(it) }
                    hide()
                    return super.onSingleTapConfirmed(e)
                }
            },
        )

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    interface Callback {
        fun onClick(content: String)
    }
}
