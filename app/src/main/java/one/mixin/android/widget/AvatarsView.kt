package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.forEachReversedWithIndex
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.User

class AvatarsView : ViewGroup {
    companion object {
        const val DEFAULT_BORDER_WIDTH = 1
        const val DEFAULT_BORDER_COLOR = Color.WHITE
        const val DEFAULT_AVATAR_SIZE = 32
        const val DEFAULT_AVATAR_RTL = false

        private const val MAX_VISIBLE_COUNT = 3
    }

    private var data = arrayListOf<Any>()

    private var borderWidth: Int
    private var borderColor: Int

    private var avatarSize: Int = 0
    private var rtl: Boolean
    private val ratio
        get() = if (isUser()) 3f / 4 else 1f / 3

    constructor(context: Context) : this(context, null)
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

    fun addList(list: List<Any>) {
        data.clear()
        data.addAll(list)
        initWithList()
    }

    fun initParams(
        borderWith: Int,
        avatarSize: Int,
    ) {
        this.borderWidth = borderWith.dp
        this.avatarSize = avatarSize.dp
    }

    fun setRTL(rtl: Boolean) {
        this.rtl = rtl
        overView?.setRTL(rtl)
        requestLayout()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val parentWidth = avatarSize + (childCount - 1) * avatarSize * ratio
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
        for (i in 0 until childCount) {
            val index =
                if (rtl) {
                    i
                } else {
                    childCount - i - 1
                }
            val c = getChildAt(i)
            val offsetLeft = index * offset
            c.layout(
                offsetLeft.toInt(),
                0,
                (offsetLeft + avatarSize).toInt(),
                measuredHeight,
            )
        }
    }

    private fun isUser(): Boolean = data.isNotEmpty() && data[0] is User

    private var overView: OverView? = null

    private fun initWithList() {
        removeAllViews()
        overView = null
        val overSize = data.size > MAX_VISIBLE_COUNT
        if (overSize) {
            val overView =
                if (isUser()) {
                    getTextView(data.size - MAX_VISIBLE_COUNT + 1)
                } else {
                    getOverView(context, rtl).apply {
                        overView = this
                    }
                }
            addView(overView)
        }
        val takeCount = if (overSize) MAX_VISIBLE_COUNT - 1 else data.size
        data.toMutableList()
            .take(takeCount)
            .forEachReversedWithIndex { _, t ->
                if (t is User) {
                    val avatarView =
                        AvatarView(context).apply {
                            setBorderWidth(borderWidth)
                            setBorderColor(borderColor)
                        }
                    avatarView.avatarSimple.setCircleBackgroundColorResource(R.color.white)
                    addView(avatarView)
                    avatarView.setInfo(t.fullName, t.avatarUrl, t.userId)
                } else if (t is String) {
                    val circleView =
                        CircleImageView(context).apply {
                            borderWidth = this@AvatarsView.borderWidth
                            borderColor = this@AvatarsView.borderColor
                        }
                    addView(circleView)
                    circleView.loadImage(t, R.drawable.ic_link_place_holder)
                }
            }
    }

    @SuppressLint("SetTextI18n")
    private fun getTextView(num: Int) =
        TextView(context).apply {
            text = "+$num"
            setTextColor(resources.getColor(R.color.wallet_pending_text_color, null))
            setBackgroundResource(R.drawable.bg_multisigs_gray)
            gravity = Gravity.CENTER
        }

    private fun getOverView(
        context: Context,
        rtl: Boolean,
    ) = OverView(context, rtl)

    @SuppressLint("ViewConstructor")
    class OverView(context: Context, private var rtl: Boolean) : ViewGroup(context) {
        fun setRTL(rtl: Boolean) {
            this.rtl = rtl
            requestLayout()
        }

        init {
            for (i in 0..2) {
                addView(
                    ImageView(context).apply {
                        setBackgroundResource(R.drawable.bg_multisigs_gray)
                    },
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
            val offset = measuredWidth / 6
            for (i in 0 until childCount) {
                if (rtl) {
                    getChildAt(i).layout(
                        0 + offset * i,
                        0,
                        measuredWidth + offset * i,
                        measuredHeight,
                    )
                } else {
                    getChildAt(i).layout(
                        0 - i * offset,
                        0,
                        measuredWidth - i * offset,
                        measuredHeight,
                    )
                }
            }
        }
    }
}
