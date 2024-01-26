package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ViewAvatarGroupBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.App

@SuppressLint("CustomViewStyleable")
class AvatarGroup
    @JvmOverloads
    constructor(
        context: Context,
        val attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) :
    RelativeLayout(context, attrs, defStyle) {
        private val binding = ViewAvatarGroupBinding.inflate(LayoutInflater.from(context), this)

        init {
            attrs?.let {
                attrs
                val ta = context.obtainStyledAttributes(attrs, R.styleable.AvatarsGroup)
                val size =
                    ta.getDimensionPixelSize(
                        R.styleable.AvatarsGroup_avatar_group_size,
                        24.dp,
                    )
                val margin = ta.getDimensionPixelSize(R.styleable.AvatarsGroup_avatar_group_margin, 16.dp)
                setSize(size, margin)
                val color = ta.getColor(R.styleable.AvatarsGroup_avatar_group_border_color, context.colorFromAttribute(R.attr.bg_gray_light))
                binding.avatar1.borderColor = color
                binding.avatar2.borderColor = color
                binding.avatar3.borderColor = color
                binding.avatar4.borderColor = color
                binding.avatar5.borderColor = color
                ta.recycle()
            }
        }

        fun setSize(
            size: Int,
            margin: Int,
        ) {
            binding.avatar1.layoutParams =
                (binding.avatar1.layoutParams as MarginLayoutParams).apply {
                    width = size
                    height = size
                }
            binding.avatar2.layoutParams =
                (binding.avatar2.layoutParams as MarginLayoutParams).apply {
                    marginStart = margin
                    width = size
                    height = size
                }
            binding.avatar3.layoutParams =
                (binding.avatar3.layoutParams as MarginLayoutParams).apply {
                    marginStart = margin * 2
                    width = size
                    height = size
                }
            binding.avatar4.layoutParams =
                (binding.avatar4.layoutParams as MarginLayoutParams).apply {
                    marginStart = margin * 3
                    width = size
                    height = size
                }
            binding.avatar5.layoutParams =
                (binding.avatar5.layoutParams as MarginLayoutParams).apply {
                    marginStart = margin * 4
                    width = size
                    height = size
                }
        }

        fun setUrls(urls: List<String?>) {
            when {
                urls.size >= 5 -> {
                    binding.avatar1.isVisible = true
                    binding.avatar2.isVisible = true
                    binding.avatar3.isVisible = true
                    binding.avatar4.isVisible = true
                    binding.avatar5.isVisible = true
                    binding.avatar1.loadImage(urls[0], R.drawable.ic_avatar_place_holder)
                    binding.avatar2.loadImage(urls[1], R.drawable.ic_avatar_place_holder)
                    binding.avatar3.loadImage(urls[2], R.drawable.ic_avatar_place_holder)
                    binding.avatar4.loadImage(urls[3], R.drawable.ic_avatar_place_holder)
                    binding.avatar5.loadImage(urls[4], R.drawable.ic_avatar_place_holder)
                }
                urls.size >= 4 -> {
                    binding.avatar1.isVisible = true
                    binding.avatar2.isVisible = true
                    binding.avatar3.isVisible = true
                    binding.avatar4.isVisible = true
                    binding.avatar5.isVisible = false
                    binding.avatar1.loadImage(urls[0], R.drawable.ic_avatar_place_holder)
                    binding.avatar2.loadImage(urls[1], R.drawable.ic_avatar_place_holder)
                    binding.avatar3.loadImage(urls[2], R.drawable.ic_avatar_place_holder)
                    binding.avatar4.loadImage(urls[3], R.drawable.ic_avatar_place_holder)
                }
                urls.size >= 3 -> {
                    binding.avatar1.isVisible = true
                    binding.avatar2.isVisible = true
                    binding.avatar3.isVisible = true
                    binding.avatar4.isVisible = false
                    binding.avatar5.isVisible = false
                    binding.avatar1.loadImage(urls[0], R.drawable.ic_avatar_place_holder)
                    binding.avatar2.loadImage(urls[1], R.drawable.ic_avatar_place_holder)
                    binding.avatar3.loadImage(urls[2], R.drawable.ic_avatar_place_holder)
                }
                urls.size == 2 -> {
                    binding.avatar1.isVisible = true
                    binding.avatar2.isVisible = true
                    binding.avatar3.isVisible = false
                    binding.avatar4.isVisible = false
                    binding.avatar5.isVisible = false
                    binding.avatar1.loadImage(urls[0], R.drawable.ic_avatar_place_holder)
                    binding.avatar2.loadImage(urls[1], R.drawable.ic_avatar_place_holder)
                }
                urls.size == 1 -> {
                    binding.avatar1.isVisible = true
                    binding.avatar2.isVisible = false
                    binding.avatar3.isVisible = false
                    binding.avatar4.isVisible = false
                    binding.avatar5.isVisible = false
                    binding.avatar1.loadImage(urls[0], R.drawable.ic_avatar_place_holder)
                }
                else -> {
                    binding.avatar1.isVisible = false
                    binding.avatar2.isVisible = false
                    binding.avatar3.isVisible = false
                    binding.avatar4.isVisible = false
                    binding.avatar5.isVisible = false
                }
            }
        }

        fun setApps(apps: List<App>) {
            setUrls(
                apps.map {
                    it.iconUrl
                },
            )
        }
    }
