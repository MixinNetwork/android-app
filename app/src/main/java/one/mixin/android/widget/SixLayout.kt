package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import kotlinx.android.synthetic.main.view_six.view.*
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.round
import one.mixin.android.ui.web.WebClip

class SixLayout : ConstraintLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var layouts: List<LinearLayout>
    private var thumbs: List<ImageView>
    private var titles: List<TextView>
    private var titlesLayouts: List<RelativeLayout>
    private var avatars: List<CircleImageView>

    init {
        LayoutInflater.from(context).inflate(R.layout.view_six, this, true)
        layouts = listOf(
            thumbnail_layout_1,
            thumbnail_layout_2,
            thumbnail_layout_3,
            thumbnail_layout_4,
            thumbnail_layout_5,
            thumbnail_layout_6
        )
        thumbs = listOf(
            thumbnail_iv_1,
            thumbnail_iv_2,
            thumbnail_iv_3,
            thumbnail_iv_4,
            thumbnail_iv_5,
            thumbnail_iv_6
        )
        avatars = listOf(
            avatar_1,
            avatar_2,
            avatar_3,
            avatar_4,
            avatar_5,
            avatar_6
        )
        titles = listOf(
            title_1,
            title_2,
            title_3,
            title_4,
            title_5,
            title_6,
        )
        titlesLayouts = listOf(
            title_layout_1,
            title_layout_2,
            title_layout_3,
            title_layout_4,
            title_layout_5,
            title_layout_6
        )
        thumbnail_layout_1.round(8.dp)
        thumbnail_layout_2.round(8.dp)
        thumbnail_layout_3.round(8.dp)
        thumbnail_layout_4.round(8.dp)
        thumbnail_layout_5.round(8.dp)
        thumbnail_layout_6.round(8.dp)
        close_1.setOnClickListener {
            onCloseListener?.onClose(0)
        }
        close_2.setOnClickListener {
            onCloseListener?.onClose(1)
        }
        close_3.setOnClickListener {
            onCloseListener?.onClose(2)
        }
        close_4.setOnClickListener {
            onCloseListener?.onClose(3)
        }
        close_5.setOnClickListener {
            onCloseListener?.onClose(4)
        }
        close_6.setOnClickListener {
            onCloseListener?.onClose(5)
        }
    }

    fun loadData(clips: List<WebClip>, expandAction: (Int) -> Unit) {
        repeat(6) { index ->
            if (index < clips.size) {
                clips[index].app.notNullWithElse(
                    { app ->
                        avatars[index].loadImage(app.iconUrl, R.drawable.ic_link_place_holder, true)
                    },
                    {
                        avatars[index].setImageResource(R.drawable.ic_link_place_holder)
                    }
                )
                titles[index].text = clips[index].name
                if (isDark(clips[index].titleColor)) {
                    titles[index].setTextColor(Color.WHITE)
                } else {
                    titles[index].setTextColor(Color.BLACK)
                }
                titlesLayouts[index].setBackgroundColor(clips[index].titleColor)
                layouts[index].visibility = View.VISIBLE
                thumbs[index].setImageBitmap(clips[index].thumb)
                layouts[index].setOnClickListener {
                    expandAction(index)
                }
            } else {
                layouts[index].visibility = View.INVISIBLE
            }
        }
    }

    private fun isDark(color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    fun setOnCloseListener(onCloseListener: OnCloseListener?) {
        this.onCloseListener = onCloseListener
    }

    private var onCloseListener: OnCloseListener? = null

    interface OnCloseListener {
        fun onClose(index: Int)
    }
}
