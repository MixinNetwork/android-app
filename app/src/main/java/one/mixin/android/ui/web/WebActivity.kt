package one.mixin.android.ui.web

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.ActivityWebBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.blurBitmap
import one.mixin.android.extension.isDarkColor
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.supportsS
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.App
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.generateConversationId
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
                },
            )
        }

        fun show(
            context: Context,
            url: String,
            conversationId: String?,
            app: App? = null,
            appCard: AppCardData? = null,
            saveName: Boolean? = null
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
                            putString(
                                WebFragment.CONVERSATION_ID,
                                conversationId ?: app?.let {
                                    generateConversationId(Session.getAccountId()!!, it.appId)
                                },
                            )
                            putParcelable(WebFragment.ARGS_APP, app)
                            putParcelable(WebFragment.ARGS_APP_CARD, appCard)
                            putBoolean(WebFragment.ARGS_SAVE_NAME, saveName?:false)
                        },
                    )
                },
            )
        }
    }

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Blur

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Blur

    private lateinit var binding: ActivityWebBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.extras != null) {
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
        } else {
            overridePendingTransition(R.anim.fade_in, R.anim.stay)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getScreenshot()?.let {
            supportsS({
                binding.background.background = BitmapDrawable(resources, it)
                binding.background.setRenderEffect(RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.MIRROR))
            }, {
                binding.container.background = BitmapDrawable(resources, it.blurBitmap(25))
            })
        }
        binding.container.setOnClickListener {
            onBackPressed()
        }

        binding.six.setOnCloseListener(
            object : SixLayout.OnCloseListener {
                override fun onClose(index: Int) {
                    releaseClip(index)
                    supportFragmentManager.findFragmentByTag(WebFragment.TAG)?.run {
                        val fragment = (this as WebFragment)
                        fragment.resetIndex(index)
                    }
                    binding.six.loadData(clips, loadViewAction)
                }
            },
        )

        binding.clear.setOnClickListener {
            alertDialogBuilder()
                .setMessage(getString(R.string.Remove_all_floats))
                .setNegativeButton(R.string.Cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.Confirm) { _, _ ->
                    releaseAll()
                    onBackPressed()
                }
                .show()
        }

        binding.close.setOnClickListener {
            onBackPressed()
        }
        handleExtras(intent)
    }

    override fun onBackPressed() {
        if (!isExpand) {
            val f = supportFragmentManager.findFragmentByTag(WebFragment.TAG)
            if (f != null) {
                showClip()
                isExpand = true
                supportFragmentManager.beginTransaction().show(f).commit()
                if (f is WebFragment) {
                    val dark = isDarkColor(f.titleColor)
                    window.statusBarColor = f.titleColor
                    SystemUIManager.lightUI(window, !dark)
                }
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        saveClips()
    }

    private fun hideWeb() {
        supportFragmentManager.findFragmentByTag(WebFragment.TAG)?.let {
            supportFragmentManager.beginTransaction().hide(it).commit()
        }
    }

    private fun releaseWeb() {
        supportFragmentManager.findFragmentByTag(WebFragment.TAG)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    private var loadViewAction = fun(index: Int) {
        val extras = Bundle()
        val clip = clips[index]
        extras.putString(WebFragment.URL, clip.url)
        extras.putString(WebFragment.CONVERSATION_ID, clip.conversationId)
        extras.putInt(WebFragment.ARGS_INDEX, index)
        extras.putParcelable(WebFragment.ARGS_APP, clip.app)
        clip.shareable?.let { extras.putBoolean(WebFragment.ARGS_SHAREABLE, it) }
        isExpand = true

        window.statusBarColor =
            clip.titleColor.apply {
                val dark = isDarkColor(this)
                SystemUIManager.lightUI(window, !dark)
            }
        releaseWeb()
        supportFragmentManager.beginTransaction().add(
            R.id.container,
            WebFragment.newInstance(extras),
            WebFragment.TAG,
        ).commit()
    }

    private fun handleExtras(intent: Intent) {
        binding.six.loadData(clips, loadViewAction)
        val extras = intent.extras
        if (extras != null) {
            isExpand = true
            releaseWeb()
            supportFragmentManager.beginTransaction().add(
                R.id.container,
                WebFragment.newInstance(extras),
                WebFragment.TAG,
            ).commit()
        } else {
            isExpand = false
            FloatingWebClip.getInstance(this.isNightMode()).hide()
            SystemUIManager.lightUI(window, !isNightMode())
            hideWeb()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleExtras(intent)
    }

    private var isExpand = false

    override fun finish() {
        collapse()
        super.finish()
        if (isExpand) {
            overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
        } else {
            overridePendingTransition(R.anim.stay, R.anim.fade_out)
        }
    }
}
