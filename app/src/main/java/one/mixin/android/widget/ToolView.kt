package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import one.mixin.android.databinding.ViewToolBinding

class ToolView constructor(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    private val binding = ViewToolBinding.inflate(LayoutInflater.from(context), this, true)
    val closeIv = binding.closeIv
    val copyIv = binding.copyIv
    val countTv = binding.countTv
    val deleteIv = binding.deleteIv
    val addStickerIv = binding.addStickerIv
    val replyIv = binding.replyIv
    val forwardIv = binding.forwardIv
    val shareIv = binding.shareIv
    val pinIv = binding.pinIv
}
