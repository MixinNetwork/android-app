package one.mixin.android.ui.imageeditor

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemColorPaleteeBinding
import one.mixin.android.extension.dp

class ColorPaletteAdapter(
    @ColorInt checked: Int,
    private val onColorChanged: (color: Int) -> Unit,
) : ListAdapter<Int, ColorPaletteAdapter.ColorHolder>(
    object : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem

        override fun areContentsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
    }
) {
    private val size = 24.dp
    private val margin = 10.dp

    var checkedColor = checked
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (value == field) return

            field = value
            notifyDataSetChanged()
            onColorChanged.invoke(value)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorHolder {
        val binding = ItemColorPaleteeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.apply {
            layoutParams = RecyclerView.LayoutParams(size, size).apply {
                setMargins(this@ColorPaletteAdapter.margin)
            }
        }
        return ColorHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    private fun getCircleBg(@ColorInt bg: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        setColor(bg)
    }

    inner class ColorHolder(private val binding: ItemColorPaleteeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(color: Int) {
            binding.apply {
                bg.background = getCircleBg(color)
                checkIv.isVisible = color == checkedColor
                bg.setOnClickListener {
                    checkedColor = color
                }
            }
        }
    }
}
