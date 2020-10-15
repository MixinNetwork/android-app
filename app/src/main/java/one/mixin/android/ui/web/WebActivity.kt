package one.mixin.android.ui.web

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_web.*
import one.mixin.android.R
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.withArgs
import org.jetbrains.anko.support.v4.withArguments

class WebActivity : AppCompatActivity() {

    companion object {
        val fragments = listOf(
            WebFragment().withArgs {
                putInt("index", 0)
            },
            WebFragment().withArgs {
                putInt("index", 1)
            },
            WebFragment().withArgs {
                putInt("index", 2)
            },
            WebFragment().withArgs {
                putInt("index", 3)
            },

            )
        val webViews = mutableListOf<WebView>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        repeat(4) {
            webViews.add(WebView(this).apply {
                loadUrl("https://uigradients.com/")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
            })
        }
        v1.setOnClickListener {
            supportFragmentManager.findFragmentByTag("0").notNullWithElse({
                supportFragmentManager.beginTransaction().show(fragments[0]).commit()
            }, {
                supportFragmentManager.beginTransaction().add(R.id.container, fragments[0], "0").commit()
            })
        }
        v2.setOnClickListener {
            supportFragmentManager.findFragmentByTag("1").notNullWithElse({
                supportFragmentManager.beginTransaction().show(fragments[1]).commit()
            }, {
                supportFragmentManager.beginTransaction().add(R.id.container, fragments[1], "1").commit()
            })
        }
        v3.setOnClickListener {
            supportFragmentManager.findFragmentByTag("2").notNullWithElse({
                supportFragmentManager.beginTransaction().show(fragments[2]).commit()
            }, {
                supportFragmentManager.beginTransaction().add(R.id.container, fragments[2], "2").commit()
            })
        }
        v4.setOnClickListener {
            supportFragmentManager.findFragmentByTag("3").notNullWithElse({
                supportFragmentManager.beginTransaction().show(fragments[3]).commit()
            }, {
                supportFragmentManager.beginTransaction().add(R.id.container, fragments[3], "3").commit()
            })
        }
    }
}