package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.forEachReversedWithIndex
import one.mixin.android.extension.loadImage

class FloatingAvatarsView : ViewGroup {
    companion object {
        const val DEFAULT_BORDER_WIDTH = 1
        const val DEFAULT_BORDER_COLOR = Color.WHITE
        const val DEFAULT_AVATAR_SIZE = 32
        const val DEFAULT_AVATAR_RTL = false

        private const val MAX_VISIBLE_COUNT = 3
    }

    private var data = arrayListOf<String>()

    private var borderWidth: Int
    private var borderColor: Int

    private var avatarSize: Int = 0
    private var rtl: Boolean
    private val ratio = 1f / 3
    private val overRatio = 1f / 6

    constructor(context: Context) : this(context, null)

    @SuppressLint("CustomViewStyleable")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AvatarsView)

        borderWidth =
            ta.getDimensionPixelSize(
                R.styleable.AvatarsView_avatar_border_width,
                DEFAULT_BORDER_WIDTH,
            )
        borderColor = ta.getColor(R.styleable.AvatarsView_avatar_border_color, DEFAULT_BORDER_COLOR)
        avatarSize =
            ta.getDimensionPixelSize(R.styleable.AvatarsView_avatar_size, DEFAULT_AVATAR_SIZE)
        rtl = ta.getBoolean(R.styleable.AvatarsView_avatar_rtl, DEFAULT_AVATAR_RTL)
        ta.recycle()
    }

    fun addList(list: List<String>) {
        data.clear()
        data.addAll(list)
        initWithList()
    }

    fun initParams(
        borderWith: Int,
        avatarSize: Int,
        borderColor: Int,
    ) {
        this.borderWidth = borderWith.dp
        this.avatarSize = avatarSize.dp
        this.borderColor = borderColor
    }

    fun setRTL(rtl: Boolean) {
        this.rtl = rtl
        requestLayout()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val parentWidth =
            if (isOver()) {
                avatarSize * (1 + 1 / 3f + (1 / 6f) * 3)
            } else {
                avatarSize + (childCount - 1) * avatarSize * ratio
            }
        setMeasuredDimension(
            MeasureSpec.makeMeasureSpec(parentWidth.toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(avatarSize, MeasureSpec.EXACTLY),
        )

        for (i in 0 until childCount) {
            val c = getChildAt(i)
            c.measure(
                MeasureSpec.makeMeasureSpec(avatarSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(avatarSize, MeasureSpec.EXACTLY),
            )
        }
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        val offset = avatarSize * ratio
        val overOffset = avatarSize * overRatio
        val overSize = isOver()
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            val itemOffset: Float =
                if (overSize) {
                    when (i) {
                        0, 1, 2, 3 -> overOffset * i
                        else -> overOffset * 3 + offset
                    }
                } else {
                    i * avatarSize * ratio
                }
            if (rtl) {
                c.layout(
                    (itemOffset).toInt(),
                    0,
                    (avatarSize + itemOffset).toInt(),
                    measuredHeight,
                )
            } else {
                c.layout(
                    (measuredWidth - avatarSize - itemOffset).toInt(),
                    0,
                    (measuredWidth - itemOffset).toInt(),
                    measuredHeight,
                )
            }
        }
    }

    private fun isOver() = data.size > MAX_VISIBLE_COUNT

    private fun initWithList() {
        removeAllViews()
        val overSize = isOver()
        val takeCount = if (overSize) MAX_VISIBLE_COUNT - 1 else data.size
        if (overSize) {
            for (i in 0..2) {
                val overImageView =
                    ImageView(context).apply {
                        setBackgroundResource(R.drawable.bg_multisigs_gray)
                    }
                addView(overImageView)
            }
        }
        data.toMutableList()
            .take(takeCount)
            .forEachReversedWithIndex { _, t ->
                val circleView =
                    CircleImageView(context).apply {
                        borderWidth = this@FloatingAvatarsView.borderWidth
                        borderColor = this@FloatingAvatarsView.borderColor
                    }
                addView(circleView)
                circleView.loadImage(t, R.drawable.ic_link_place_holder)
            }
    }
}
