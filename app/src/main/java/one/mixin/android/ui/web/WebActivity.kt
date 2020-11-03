package one.mixin.android.ui.web

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.core.graphics.ColorUtils
import dagger.hilt.android.AndroidEntryPoint
import io.alterac.blurkit.BlurKit
import kotlinx.android.synthetic.main.activity_web.*
import one.mixin.android.R
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.isDarkColor
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.vo.App
import one.mixin.android.vo.AppCardData
import one.mixin.android.widget.SixLayout

@AndroidEntryPoint
class WebActivity : BaseActivity() {

    companion object {
        fun show(context: Context) {
            context.startActivity(
                Intent(context, WebActivity::class.java).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }

        fun show(
            context: Context,
            url: String,
            conversationId: String?,
            app: App? = null,
            appCard: AppCardData? = null
        ) {
            context.startActivity(
                Intent(context, WebActivity::class.java).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtras(
                        Bundle().apply {
                            putString(WebFragment.URL, url)
                            putString(WebFragment.CONVERSATION_ID, conversationId)
                            putParcelable(WebFragment.ARGS_APP, app)
                            putParcelable(WebFragment.ARGS_APP_CARD, appCard)
                        }
                    )
                }
            )
        }
    }

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Web

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Web

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.extras != null) {
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
        } else {
            overridePendingTransition(R.anim.fade_in, R.anim.stay)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        screenshot?.let {
            container.background =
                BitmapDrawable(resources, BlurKit.getInstance().blur(it, 25))
        }
        container.setOnClickListener {
            finish()
        }

        six.setOnCloseListener(object : SixLayout.OnCloseListener {
            override fun onClose(index: Int) {
                releaseClip(index)
                six.loadData(clips, loadViewAction)
            }
        })

        clear.setOnClickListener {
            alertDialogBuilder()
                .setMessage(getString(R.string.web_delete_tip))
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.confirm) { _, _ ->
                    releaseAll()
                    finish()
                }
                .show()
        }

        close.setOnClickListener {
            finish()
        }

        handleExtras(intent)
    }

    private var loadViewAction = fun(index: Int) {
        val extras = Bundle()
        val clip = clips[index]
        extras.putString(WebFragment.URL, clip.url)
        extras.putParcelable(WebFragment.ARGS_APP, clip.app)
        extras.putInt(WebFragment.ARGS_INDEX, index)
        isExpand = true


        window.statusBarColor = clip.titleColor.apply {
            val dark = isDarkColor(this)
            if (dark) {
                window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
        supportFragmentManager.beginTransaction().add(
            R.id.container,
            WebFragment.newInstance(extras),
            WebFragment.TAG
        ).commit()
    }

    private fun handleExtras(intent: Intent) {
        six.loadData(clips, loadViewAction)
        intent.extras.notNullWithElse(
            { extras ->
                isExpand = true
                supportFragmentManager.beginTransaction().add(
                    R.id.container,
                    WebFragment.newInstance(extras),
                    WebFragment.TAG
                ).commit()
            },
            {
                FloatingWebClip.getInstance().hide()
                window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                window.statusBarColor = Color.parseColor("#CC1C1C1C")
                supportFragmentManager.findFragmentByTag(WebFragment.TAG)?.let {
                    supportFragmentManager.beginTransaction().remove(it).commit()
                }
            }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleExtras(intent)
    }

    private var isExpand = false

    override fun finish() {
        collapse(this)
        super.finish()
        if (isExpand) {
            overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
        } else {
            overridePendingTransition(R.anim.stay, R.anim.fade_out)
        }
    }
}
