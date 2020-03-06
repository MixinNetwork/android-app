package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import one.mixin.android.R

class SuspiciousLinkView(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {

    private val backView: View
    private val continueView: View

    var listener: SuspiciousListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_suspicious_link, this, true)
        backView = findViewById(R.id.back_tv)
        continueView = findViewById(R.id.continue_tv)

        backView.setOnClickListener {
            listener?.onBackClick()
        }
        continueView.setOnClickListener {
            listener?.onContinueClick()
        }
    }

    interface SuspiciousListener {
        fun onBackClick()
        fun onContinueClick()
    }
}
