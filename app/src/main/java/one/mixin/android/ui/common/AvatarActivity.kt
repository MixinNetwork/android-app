package one.mixin.android.ui.common

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnPreDraw
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_avatar.*
import one.mixin.android.R
import one.mixin.android.extension.belowOreo
import one.mixin.android.widget.AvatarTransform

class AvatarActivity : BaseActivity() {

    companion object {
        const val TAG = "AvatarFragment"
        const val ARGS_URL = "args_url"

        fun show(activity: Activity, url: String, view: View) {
            val intent = Intent(activity, AvatarActivity::class.java).apply {
                putExtra(ARGS_URL, url)
            }
            val options = ActivityOptions.makeSceneTransitionAnimation(
                activity,
                view,
                activity.getString(R.string.avatar_transition_name)
            )
            activity.startActivity(intent, options.toBundle())
        }
    }

    private val url: String by lazy { intent.getStringExtra(ARGS_URL) as String }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        belowOreo {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        postponeEnterTransition()
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_avatar)

        Glide.with(this).asBitmap().load(url).listener(
            object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    avatar.doOnPreDraw {
                        val avatarTransform = AvatarTransform(resource).apply { addTarget(avatar) }
                        window.sharedElementEnterTransition = avatarTransform
                        startPostponedEnterTransition()
                    }
                    return false
                }
            }
        ).into(avatar)

        root.setOnClickListener { finish() }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(avatar, View.SCALE_X, 0f),
                ObjectAnimator.ofFloat(avatar, View.SCALE_Y, 0f),
                ObjectAnimator.ofFloat(root, View.ALPHA, 0f)
            )
            duration = 200
            interpolator = DecelerateInterpolator()
            doOnEnd { super.finish() }
        }.start()
    }
}
