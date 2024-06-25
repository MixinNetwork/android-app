package one.mixin.android.ui.common

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnPreDraw
import one.mixin.android.R
import one.mixin.android.databinding.ActivityAvatarBinding
import one.mixin.android.extension.belowOreo
import one.mixin.android.extension.blurBitmap
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.supportsS
import one.mixin.android.ui.web.getScreenshot
import one.mixin.android.ui.web.refreshScreenshot
import one.mixin.android.widget.AvatarTransform

class AvatarActivity : BaseActivity() {
    companion object {
        const val TAG = "AvatarActivity"
        const val ARGS_URL = "args_url"

        fun show(
            activity: Activity,
            url: String,
            view: View,
        ) {
            refreshScreenshot(activity)
            val intent =
                Intent(activity, AvatarActivity::class.java).apply {
                    putExtra(ARGS_URL, url)
                }
            val options =
                ActivityOptions.makeSceneTransitionAnimation(
                    activity,
                    view,
                    activity.getString(R.string.avatar_transition_name),
                )
            activity.startActivity(intent, options.toBundle())
        }
    }

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Blur

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Blur

    private val url: String by lazy { intent.getStringExtra(ARGS_URL) as String }

    private lateinit var binding: ActivityAvatarBinding

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        belowOreo {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        postponeEnterTransition()
        window.statusBarColor = Color.TRANSPARENT
        binding = ActivityAvatarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getScreenshot()?.let {
            supportsS({
                binding.background.background = BitmapDrawable(resources, it)
                binding.background.setRenderEffect(RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.MIRROR))
            }, {
                binding.rootView.background = BitmapDrawable(resources, it.blurBitmap(25))
            })
        }

        binding.avatar.loadImage(url, onSuccess = { _, result ->
            binding.avatar.doOnPreDraw {
                val bitmap = (result.drawable as BitmapDrawable).bitmap
                val avatarTransform = AvatarTransform(bitmap).apply { addTarget(binding.avatar) }
                window.sharedElementEnterTransition = avatarTransform
                startPostponedEnterTransition()
            }
        })

        binding.rootView.setOnClickListener { finish() }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.avatar, View.SCALE_X, 0f),
                ObjectAnimator.ofFloat(binding.avatar, View.SCALE_Y, 0f),
                ObjectAnimator.ofFloat(binding.rootView, View.ALPHA, 0f),
            )
            duration = 200
            interpolator = DecelerateInterpolator()
            doOnEnd { super.finish() }
        }.start()
    }
}
