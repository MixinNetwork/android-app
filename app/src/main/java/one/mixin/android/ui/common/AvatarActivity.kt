package one.mixin.android.ui.common

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnPreDraw
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_avatar.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.widget.AvatarTransform

class AvatarActivity : AppCompatActivity() {

    companion object {
        const val TAG = "AvatarFragment"
        const val ARGS_URL = "args_url"
        const val ARGS_BITMAP = "args_bitmap"

        fun show(activity: Activity, url: String, view: View, bitmap: Bitmap) {
            val intent = Intent(activity, AvatarActivity::class.java).apply {
                putExtra(ARGS_URL, url)
                putExtra(ARGS_BITMAP, bitmap)
            }
            val options = ActivityOptions.makeSceneTransitionAnimation(activity,
                view, activity.getString(R.string.avatar_transition_name))
            activity.startActivity(intent, options.toBundle())
        }
    }

    private val url: String by lazy { intent.getStringExtra(ARGS_URL) }
    private val bitmap: Bitmap by lazy { intent.getParcelableExtra(ARGS_BITMAP) as Bitmap }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_avatar)
        val avatarTransform = AvatarTransform(bitmap).apply { addTarget(avatar) }
        window.sharedElementEnterTransition = avatarTransform

        avatar.loadImage(url, object : RequestListener<Drawable?> {
            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable?>?,
                dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                avatar.doOnPreDraw {
                    startPostponedEnterTransition()
                }
                return false
            }

            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable?>?,
                isFirstResource: Boolean): Boolean {
                return false
            }
        })
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