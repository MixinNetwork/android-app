package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ViewAssetIconGroupBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage

@SuppressLint("CustomViewStyleable")
class AssetIconGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {

    private val binding = ViewAssetIconGroupBinding.inflate(LayoutInflater.from(context), this)
    private val textMore = binding.textMore

    init {
        attrs?.let {
            attrs
            val ta = context.obtainStyledAttributes(attrs, R.styleable.AssetIconGroup)
            val color = ta.getColor(R.styleable.AssetIconGroup_asset_group_border_color, context.colorFromAttribute(R.attr.bg_white))
            binding.icon1.borderColor = color
            binding.icon2.borderColor = color
            binding.icon3.borderColor = color
            ta.recycle()
        }
    }

    private fun setBorderColor(color: Int) {
        binding.icon1.borderColor = color
        binding.icon2.borderColor = color
        binding.icon3.borderColor = color
    }

    fun setSize(size: Int, margin: Int) {
        binding.icon1.layoutParams = (binding.icon1.layoutParams as MarginLayoutParams).apply {
            width = size
            height = size
        }
        binding.icon2.layoutParams = (binding.icon2.layoutParams as MarginLayoutParams).apply {
            marginStart = margin
            width = size
            height = size
        }
        binding.icon3.layoutParams = (binding.icon3.layoutParams as MarginLayoutParams).apply {
            marginStart = margin * 2
            width = size
            height = size
        }
        textMore.layoutParams = (textMore.layoutParams as MarginLayoutParams).apply {
            marginStart = margin * 3
            width = size
            height = size
        }
    }

    fun setUrls(urls: List<String?>) {
        val n = urls.size
        hideAll()
        when {
            n == 1 -> {
                showIcon(binding.icon1, urls[0])
            }

            n == 2 -> {
                showIcon(binding.icon1, urls[0])
                showIcon(binding.icon2, urls[1])
            }

            n == 3 -> {
                showIcon(binding.icon1, urls[0])
                showIcon(binding.icon2, urls[1])
                showIcon(binding.icon3, urls[2])
            }

            n > 3 -> {
                showIcon(binding.icon1, urls[0])
                showIcon(binding.icon2, urls[1])
                textMore.isVisible = true
                textMore.text = "+${n - 2}"
            }
        }
    }

    private fun showIcon(view: CircleImageView, url: String?) {
        view.isVisible = true
        view.loadImage(url, R.drawable.ic_avatar_place_holder)
    }

    private fun hideAll() {
        binding.icon1.isVisible = false
        binding.icon2.isVisible = false
        binding.icon3.isVisible = false
        textMore.isVisible = false
    }
}