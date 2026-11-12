package one.mixin.android.ui.home

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.util.Locale
import one.mixin.android.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.google.android.material.R as MaterialR

@RunWith(AndroidJUnit4::class)
class HomeNavigationLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext

    private data class Screen(val name: String, val widthDp: Int, val heightDp: Int, val densityDpi: Int)

    @Test
    fun labelsFitAndCollapsedMatchesSpec() {
        val outDir = File(targetContext.getExternalFilesDir(null), "home_navigation").apply {
            deleteRecursively()
            mkdirs()
        }
        val issues = mutableListOf<String>()
        instrumentation.runOnMainSync {
            for (screen in SCREENS) {
                for (fontScale in FONT_SCALES) {
                    for (locale in LOCALES) {
                        for (night in listOf(false, true)) {
                            val capture = locale.toLanguageTag() in CAPTURE_LOCALES && (!night || fontScale == 1f)
                            check(screen, fontScale, locale, night, if (capture) outDir else null, issues)
                        }
                    }
                }
            }
        }
        assertTrue(issues.joinToString("\n"), issues.isEmpty())
    }

    private fun check(screen: Screen, fontScale: Float, locale: Locale, night: Boolean, outDir: File?, issues: MutableList<String>) {
        val config = Configuration(targetContext.resources.configuration).apply {
            setLocale(locale)
            this.fontScale = fontScale
            densityDpi = screen.densityDpi
            screenWidthDp = screen.widthDp
            screenHeightDp = screen.heightDp
            smallestScreenWidthDp = minOf(screen.widthDp, screen.heightDp)
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val context: Context = ContextThemeWrapper(targetContext.createConfigurationContext(config), if (night) R.style.AppTheme_Night_NoActionBar else R.style.AppTheme_NoActionBar)
        val root = LayoutInflater.from(context).inflate(R.layout.activity_main, null)
        val nav = root.findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.itemIconTintList = null
        nav.isItemActiveIndicatorEnabled = false
        nav.itemBackgroundResource = if (night) R.drawable.bg_bottom_navigation_item_night else R.drawable.bg_bottom_navigation_item
        nav.setBackgroundResource(R.drawable.bg_bottom_navigation)

        val density = context.resources.displayMetrics.density
        val widthPx = (screen.widthDp * density).toInt()
        val heightPx = (screen.heightDp * density).toInt()
        val expandedHeight = nav.homeNavigationExpandedHeight()
        val tag = "${screen.name} ${screen.widthDp}x${screen.heightDp}dp ${screen.densityDpi}dpi font=$fontScale ${locale.toLanguageTag()}${if (night) " night" else ""}"
        val fileTag = "${screen.name}_font${fontScale}_${locale.toLanguageTag()}${if (night) "_night" else ""}"

        for ((state, progress) in listOf("expanded" to 0f, "collapsed" to 1f)) {
            layout(root, widthPx, heightPx)
            nav.applyHomeNavigationCollapse(progress, expandedHeight)
            layout(root, widthPx, heightPx)
            nav.applyHomeNavigationCollapse(progress, expandedHeight)
            layout(root, widthPx, heightPx)

            if (progress == 0f) {
                checkLabels(nav, density, "$tag expanded", issues)
            } else {
                checkCollapsed(nav, density, "$tag collapsed", issues)
            }
            outDir?.let { capture(root, nav, density, File(it, "${fileTag}_$state.png")) }
        }
    }

    private fun layout(root: View, widthPx: Int, heightPx: Int) {
        root.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, widthPx, heightPx)
    }

    private fun checkLabels(nav: BottomNavigationView, density: Float, tag: String, issues: MutableList<String>) {
        val pillInset = 4 * density
        nav.menu.forEach { item ->
            val itemView = nav.findViewById<View>(item.itemId)
            listOf(MaterialR.id.navigation_bar_item_small_label_view, MaterialR.id.navigation_bar_item_large_label_view)
                .mapNotNull { itemView.findViewById<TextView>(it) }
                .filter { it.isVisible && it.alpha > 0f }
                .forEach { label ->
                    val text = label.text.toString()
                    val layoutHeight = label.layout?.height ?: 0
                    if (label.height < layoutHeight) {
                        issues += "$tag '$text' squeezed: label height ${label.height} < text height $layoutHeight"
                    }
                    val labelBottom = offsetY(label, nav) + label.height
                    if (labelBottom > nav.height - pillInset) {
                        issues += "$tag '$text' label bottom $labelBottom exceeds navigation ${nav.height} minus pill inset"
                    }
                    val textWidth = label.paint.measureText(text)
                    val ellipsized = label.layout?.let { it.lineCount > 1 || it.getEllipsisCount(0) > 0 } ?: false
                    if (ellipsized || textWidth > label.width + 1) {
                        issues += "$tag '$text' truncated: text ${textWidth.toInt()} > label ${label.width}"
                    }
                }
        }
    }

    private fun checkCollapsed(nav: BottomNavigationView, density: Float, tag: String, issues: MutableList<String>) {
        val expected = (48 * density).toInt()
        if (kotlin.math.abs(nav.height - expected) > 1) {
            issues += "$tag height ${nav.height} != 48dp ($expected)"
        }
        nav.menu.forEach { item ->
            val itemView = nav.findViewById<View>(item.itemId)
            val icon = itemView.findViewById<View>(MaterialR.id.navigation_bar_item_icon_view)
            val center = offsetY(icon, nav) + icon.height / 2f
            if (kotlin.math.abs(center - nav.height / 2f) > 1.5f) {
                issues += "$tag '${item.title}' icon center $center not centered in ${nav.height}"
            }
        }
    }

    private fun offsetY(view: View, ancestor: View): Float {
        var y = 0f
        var current: View = view
        while (current !== ancestor) {
            y += current.top + current.translationY
            current = current.parent as View
        }
        return y
    }

    private fun capture(root: View, nav: BottomNavigationView, density: Float, file: File) {
        val top = (nav.top - 24 * density).toInt().coerceAtLeast(0)
        val bitmap = Bitmap.createBitmap(root.width, root.height - top, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(if (nav.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.BLACK else Color.WHITE)
        val line = Paint().apply { color = Color.GRAY; alpha = 60 }
        var y = 0f
        while (y < bitmap.height) {
            canvas.drawRect(16 * density, y + 12 * density, root.width - 16 * density, y + 20 * density, line)
            y += 36 * density
        }
        canvas.translate(nav.left.toFloat(), (nav.top - top).toFloat())
        // Software rendering ignores clipToOutline, so clip to the pill to show what the user sees.
        val radius = nav.height / 2f
        canvas.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, nav.width.toFloat(), nav.height.toFloat()), radius, radius, Path.Direction.CW) })
        nav.draw(canvas)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private companion object {
        val SCREENS = listOf(
            Screen("small", 320, 568, 320),
            Screen("compact", 360, 640, 420),
            Screen("pixel", 411, 914, 420),
            Screen("large", 430, 932, 480),
            Screen("foldable", 600, 900, 420),
        )
        val FONT_SCALES = listOf(1f, 1.3f, 2f)
        val LOCALES = listOf("en", "zh-CN", "zh-TW", "zh-HK", "ja", "ko", "ru", "es", "in", "ms").map(Locale::forLanguageTag)
        val CAPTURE_LOCALES = setOf("en", "zh-CN")
    }
}
