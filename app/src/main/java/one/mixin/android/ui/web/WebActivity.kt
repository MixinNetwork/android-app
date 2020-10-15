package one.mixin.android.ui.web

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_web.*
import one.mixin.android.R
import one.mixin.android.extension.round
import one.mixin.android.extension.withArgs
import one.mixin.android.extension.dp

class WebActivity : AppCompatActivity() {

    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, WebActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }

        // val fragments = listOf(
        //     WebFragment().withArgs {
        //         putInt("index", 0)
        //     },
        //     WebFragment().withArgs {
        //         putInt("index", 1)
        //     },
        //     WebFragment().withArgs {
        //         putInt("index", 2)
        //     },
        //     WebFragment().withArgs {
        //         putInt("index", 3)
        //     })
        val webViews = mutableListOf<WebView>()
    }

    private lateinit var layouts: List<FrameLayout>
    private lateinit var thumbs: List<ImageView>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in_bottom, 0)
        setContentView(R.layout.activity_web)
        // repeat(4) {
        //     webViews.add(WebView(this).apply {
        //         loadUrl("https://uigradients.com/")
        //         settings.javaScriptEnabled = true
        //         settings.domStorageEnabled = true
        //         settings.useWideViewPort = true
        //         settings.loadWithOverviewMode = true
        //     })
        // }
        //
        layouts = listOf(thumbnail_layout_1, thumbnail_layout_2, thumbnail_layout_3, thumbnail_layout_4, thumbnail_layout_5, thumbnail_layout_6)
        thumbs = listOf(thumbnail_iv_1, thumbnail_iv_2, thumbnail_iv_3, thumbnail_iv_4, thumbnail_iv_5, thumbnail_iv_6)
        thumbnail_layout_1.round(8.dp)
        thumbnail_layout_2.round(8.dp)
        thumbnail_layout_3.round(8.dp)
        thumbnail_layout_4.round(8.dp)
        thumbnail_layout_5.round(8.dp)
        thumbnail_layout_6.round(8.dp)

        repeat(6) { index ->
            if (index < clips.size) {
                layouts[index].isVisible = true
                thumbs[index].setImageBitmap(clips.valueAt(index).thumb)
            } else {
                layouts[index].isVisible = false
            }
        }

    }

    override fun finish() {
        super.finish()
        collapse()
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }
}