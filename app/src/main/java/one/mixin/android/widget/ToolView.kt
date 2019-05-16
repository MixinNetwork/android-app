package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import one.mixin.android.R
import one.mixin.android.extension.dpToPx

class ToolView constructor(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_tool, this, true)
        setBackgroundColor(Color.WHITE)
        elevation = context.dpToPx(4f).toFloat()
    }
}
