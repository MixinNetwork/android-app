package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.view_blur_avatar.view.*
import one.mixin.android.R
import timber.log.Timber

class BlurAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_blur_avatar, this, true)
    }

    fun showBlur(v: View) {
        post {
            val start = System.currentTimeMillis()
            Blurry.with(context).capture(v).into(blur_civ)
            visibility = View.VISIBLE
            Timber.d("blur cost ${System.currentTimeMillis() - start}")
        }
    }

    fun hideBlur() {
        visibility = View.GONE
    }
}