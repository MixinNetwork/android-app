package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import kotlinx.android.synthetic.main.view_avatar.view.*
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.User
import org.jetbrains.anko.collections.forEachReversedWithIndex
import kotlin.math.abs

class AvatarsView : ViewGroup {
    companion object {
        const val DEFAULT_BORDER_WIDTH = 1
        const val DEFAULT_BORDER_COLOR = Color.WHITE
        const val DEFAULT_AVATAR_SIZE = 32

        private const val MAX_VISIBLE_COUNT = 3
    }

    private var data = arrayListOf<Any>()

    private var borderWidth: Int
    private var borderColor: Int

    private var avatarSize: Int = 0
    private var ratio = 1f / 2

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AvatarsView)

        borderWidth = ta.getDimensionPixelSize(
            R.styleable.AvatarsView_avatar_border_width,
            DEFAULT_BORDER_WIDTH
        )
        borderColor = ta.getColor(R.styleable.AvatarsView_avatar_border_color, DEFAULT_BORDER_COLOR)
        avatarSize =
            ta.getDimensionPixelSize(R.styleable.AvatarsView_avatar_size, DEFAULT_AVATAR_SIZE)

        ta.recycle()
    }

    fun addList(list: List<Any>) {
        data.clear()
        data.addAll(list)
        initWithList()
    }

    fun initParams(borderWith: Int, avatarSize: Int) {
        this.borderWidth = borderWith.dp
        this.avatarSize = avatarSize.dp
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = avatarSize + (childCount - 1) * avatarSize * ratio
        setMeasuredDimension(
            MeasureSpec.makeMeasureSpec(parentWidth.toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(avatarSize, MeasureSpec.EXACTLY)
        )

        for (i in 0 until childCount) {
            val c = getChildAt(i)
            c.measure(
                MeasureSpec.makeMeasureSpec(avatarSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(avatarSize, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            val offsetLeft = (childCount - i - 1) * avatarSize * ratio
            val offsetRight = i * avatarSize * ratio
            c.layout(
                offsetLeft.toInt(),
                t - marginTop,
                (r - offsetRight - l).toInt(),
                b - marginBottom
            )
        }
    }

    private fun initWithList() {
        removeAllViews()
        val overSize = data.size > MAX_VISIBLE_COUNT
        if (overSize) {
            val overView = getOverView()
            addView(overView)
        }
        val takeCount = if (overSize) MAX_VISIBLE_COUNT - 1 else data.size
        data.toMutableList()
            .take(takeCount)
            .forEachReversedWithIndex { _, t ->
                if (t is User) {
                    val avatarView = AvatarView(context).apply {
                        setBorderWidth(borderWidth)
                        setBorderColor(borderColor)
                    }
                    avatarView.avatar_simple.setCircleBackgroundColorResource(R.color.white)
                    addView(avatarView)
                    avatarView.setInfo(t.fullName, t.avatarUrl, t.userId)
                } else if (t is String) {
                    val circleView = CircleImageView(context).apply {
                        borderWidth = this@AvatarsView.borderWidth
                        borderColor = this@AvatarsView.borderColor
                    }
                    addView(circleView)
                    circleView.loadImage(t, R.drawable.ic_link_place_holder, true)
                }
            }
    }

    private fun getOverView() = object : ViewGroup(context) {

        init {
            for (i in 0..2) {
                addView(ImageView(context).apply {
                    setBackgroundResource(R.drawable.bg_multisigs_gray)
                })
            }
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            val offset = measuredWidth / 6
            for (i in 0 until childCount) {
                getChildAt(i).layout(
                    measuredWidth - offset * i - measuredWidth,
                    0,
                    measuredWidth - offset * i,
                    measuredHeight
                )
            }
        }
    }
}
