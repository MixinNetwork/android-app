package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import one.mixin.android.R

class ToolView constructor(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_tool, this, true)
        setBackgroundColor(Color.WHITE)
    }
}
