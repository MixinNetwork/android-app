package one.mixin.android.ui.landing

import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import one.mixin.android.R
import one.mixin.android.databinding.ActivityLandingBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.isCompactWindow

internal fun FragmentActivity.preventLandingDismiss() {
    val preventDismiss = object : OnBackPressedCallback(supportFragmentManager.backStackEntryCount == 0) {
        override fun handleOnBackPressed() {
            if (isCompactWindow()) {
                isEnabled = false
                try {
                    onBackPressedDispatcher.onBackPressed()
                } finally {
                    isEnabled = supportFragmentManager.backStackEntryCount == 0
                }
            }
        }
    }
    onBackPressedDispatcher.addCallback(this, preventDismiss)
    supportFragmentManager.addOnBackStackChangedListener {
        preventDismiss.isEnabled = supportFragmentManager.backStackEntryCount == 0
    }
}

fun ActivityLandingBinding.configurePanel() {
    val background = GradientDrawable()
    root.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
        val width = right - left
        val height = bottom - top
        val expanded = minOf(width, height) >= 600.dp
        val panelWidth = if (expanded) minOf(480.dp, width - 48.dp) else width
        val panelHeight = if (expanded) minOf(720.dp, (height - 48.dp).coerceAtLeast(0)) else height
        if (container.layoutParams.width != panelWidth || container.layoutParams.height != panelHeight) {
            container.updateLayoutParams<FrameLayout.LayoutParams> {
                this.width = panelWidth
                this.height = panelHeight
            }
        }
        if (expanded) {
            background.setColor(root.context.colorFromAttribute(R.attr.bg_white))
            background.cornerRadius = 24.dp.toFloat()
            container.background = background
            container.clipToOutline = true
            container.elevation = 12.dp.toFloat()
            root.setBackgroundColor(root.context.colorFromAttribute(R.attr.bg_window))
        } else {
            container.background = null
            container.clipToOutline = false
            container.elevation = 0f
            root.background = null
        }
    }
}
