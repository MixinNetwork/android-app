package one.mixin.android.tip.test

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ViewAnimator
import androidx.core.view.isVisible
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.databinding.ViewDebugBinding
import one.mixin.android.tip.test.TroubleMarker.STOP_CREATE_PIN
import one.mixin.android.tip.test.TroubleMarker.STOP_NODE_SIGN
import one.mixin.android.tip.test.TroubleMarker.STOP_SAVE_AES

class DebugView(context: Context, attributeSet: AttributeSet) : ViewAnimator(context, attributeSet) {

    private val binding = ViewDebugBinding.inflate(LayoutInflater.from(context), this)
    init {
        isVisible = BuildConfig.DEBUG
        binding.show.setOnClickListener {
            displayedChild = 1
        }
        binding.hide.setOnClickListener {
            displayedChild = 0
        }
        binding.group.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio1 -> TroubleMarker.enableStop(STOP_NODE_SIGN)
                R.id.radio2 -> TroubleMarker.enableStop(STOP_CREATE_PIN)
                R.id.radio3 -> TroubleMarker.enableStop(STOP_SAVE_AES)
            }
        }
    }
}
