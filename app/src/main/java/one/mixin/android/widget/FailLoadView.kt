package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewFailLoadBinding

class FailLoadView(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {

    private val binding = ViewFailLoadBinding.inflate(LayoutInflater.from(context), this, true)
    val contactTv get() = binding.contactTv
    val webFailDescription get() = binding.webFailDescription
    var listener: FailLoadListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_fail_load, this, true)

        binding.reloadTv.setOnClickListener {
            listener?.onReloadClick()
        }
        binding.contactTv.setOnClickListener {
            listener?.onContactClick()
        }
    }

    interface FailLoadListener {
        fun onReloadClick()
        fun onContactClick()
    }
}
