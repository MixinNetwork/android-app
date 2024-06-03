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
import one.mixin.android.R
import one.mixin.android.databinding.ViewSixBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.isDarkColor
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.round
import one.mixin.android.ui.web.WebClip

class SixLayout : ConstraintLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    private var layouts: List<LinearLayout>
    private var thumbs: List<ImageView>
    private var titles: List<TextView>
    private var titlesLayouts: List<RelativeLayout>
    private var avatars: List<CircleImageView>
    private val binding = ViewSixBinding.inflate(LayoutInflater.from(context), this)

    init {
        layouts =
            listOf(
                binding.thumbnailLayout1,
                binding.thumbnailLayout2,
                binding.thumbnailLayout3,
                binding.thumbnailLayout4,
                binding.thumbnailLayout5,
                binding.thumbnailLayout6,
            )
        thumbs =
            listOf(
                binding.thumbnailIv1,
                binding.thumbnailIv2,
                binding.thumbnailIv3,
                binding.thumbnailIv4,
                binding.thumbnailIv5,
                binding.thumbnailIv6,
            )
        avatars =
            listOf(
                binding.avatar1,
                binding.avatar2,
                binding.avatar3,
                binding.avatar4,
                binding.avatar5,
                binding.avatar6,
            )
        titles =
            listOf(
                binding.title1,
                binding.title2,
                binding.title3,
                binding.title4,
                binding.title5,
                binding.title6,
            )
        titlesLayouts =
            listOf(
                binding.titleLayout1,
                binding.titleLayout2,
                binding.titleLayout3,
                binding.titleLayout4,
                binding.titleLayout5,
                binding.titleLayout6,
            )
        binding.thumbnailLayout1.round(8.dp)
        binding.thumbnailLayout2.round(8.dp)
        binding.thumbnailLayout3.round(8.dp)
        binding.thumbnailLayout4.round(8.dp)
        binding.thumbnailLayout5.round(8.dp)
        binding.thumbnailLayout6.round(8.dp)
        binding.close1.setOnClickListener {
            onCloseListener?.onClose(0)
        }
        binding.close2.setOnClickListener {
            onCloseListener?.onClose(1)
        }
        binding.close3.setOnClickListener {
            onCloseListener?.onClose(2)
        }
        binding.close4.setOnClickListener {
            onCloseListener?.onClose(3)
        }
        binding.close5.setOnClickListener {
            onCloseListener?.onClose(4)
        }
        binding.close6.setOnClickListener {
            onCloseListener?.onClose(5)
        }
    }

    fun loadData(
        clips: List<WebClip>,
        expandAction: (Int) -> Unit,
    ) {
        repeat(6) { index ->
            if (index < clips.size) {
                val app = clips[index].app
                if (app != null) {
                    avatars[index].loadImage(app.iconUrl, R.drawable.ic_link_place_holder)
                } else {
                    avatars[index].setImageResource(R.drawable.ic_link_place_holder)
                }
                titles[index].text = clips[index].name
                if (isDarkColor(clips[index].titleColor)) {
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

    fun setOnCloseListener(onCloseListener: OnCloseListener?) {
        this.onCloseListener = onCloseListener
    }

    private var onCloseListener: OnCloseListener? = null

    interface OnCloseListener {
        fun onClose(index: Int)
    }
}
