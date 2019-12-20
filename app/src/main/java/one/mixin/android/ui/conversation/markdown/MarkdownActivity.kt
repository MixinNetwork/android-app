package one.mixin.android.ui.conversation.markdown

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_markdown.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.style.MarkwonUtil
import one.mixin.android.util.SystemUIManager
import org.jetbrains.anko.configuration

class MarkdownActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isNightMode()) {
            setTheme(getNightThemeId())
            SystemUIManager.lightUI(window, false)
        } else {
            setTheme(getDefaultThemeId())
            SystemUIManager.lightUI(window, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = colorFromAttribute(R.attr.bg_white)
        }
        setContentView(R.layout.activity_markdown)

        val markdown = intent.getStringExtra(CONTENT) ?: return
        MarkwonUtil.getSingle().setMarkdown(tv, markdown)
    }

    private fun isNightMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        } else {
            defaultSharedPreferences.getInt(
                Constants.Theme.THEME_CURRENT_ID,
                Constants.Theme.THEME_DEFAULT_ID
            ) == Constants.Theme.THEME_NIGHT_ID
        }
    }

    private fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_NoActionBar
    }

    private fun getDefaultThemeId(): Int {
        return R.style.AppTheme_NoActionBar
    }

    companion object {
        private const val CONTENT = "content"
        fun show(context: Context, content: String) {
            context.startActivity(Intent(context, MarkdownActivity::class.java).apply {
                putExtra(CONTENT, content)
            })
        }
    }
}
