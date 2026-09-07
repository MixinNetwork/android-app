package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import one.mixin.android.R

class HomeToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {
    private val titleView: TextView
    private val searchButton: ImageButton
    private val scanButton: ImageButton
    private val settingsButton: ImageButton

    init {
        LayoutInflater.from(context).inflate(R.layout.view_home_toolbar, this, true)
        titleView = findViewById(R.id.home_toolbar_title)
        searchButton = findViewById(R.id.home_toolbar_search)
        scanButton = findViewById(R.id.home_toolbar_scan)
        settingsButton = findViewById(R.id.home_toolbar_settings)

        context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.text)).apply {
            getText(0)?.let(titleView::setText)
            recycle()
        }
    }

    fun setTitle(title: CharSequence) {
        titleView.text = title
    }

    fun setOnSearchClickListener(listener: View.OnClickListener?) {
        searchButton.setOnClickListener(listener)
    }

    fun setOnScanClickListener(listener: View.OnClickListener?) {
        scanButton.setOnClickListener(listener)
    }

    fun setOnSettingsClickListener(listener: View.OnClickListener?) {
        settingsButton.setOnClickListener(listener)
    }
}
