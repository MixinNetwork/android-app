package one.mixin.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_blur_avatar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.extension.fastBlur
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

class BlurAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_blur_avatar, this, true)
    }

    private val blurContext: CoroutineContext = Job()

    private var blurCache: WeakReference<Bitmap>? = null
    private var blurUnique: String? = null

    fun showBlur(v: View, unique: String) {
        if (blurCache == null || blurCache!!.get() == null || unique != blurUnique) {
            GlobalScope.launch(blurContext) {
                GlobalScope.launch(blurContext) {
                    var bitmap = Bitmap.createBitmap(v.layoutParams.width, v.layoutParams.height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    val c = Canvas(bitmap)
                    v.layout(v.left, v.top, v.right, v.bottom)
                    v.draw(c)
                    val start = System.currentTimeMillis()
                    bitmap = bitmap.fastBlur(1f, 60)
                    Timber.d("blur cost ${System.currentTimeMillis() - start}")
                    withContext(Dispatchers.Main) {
                        bitmap?.let { bitmap ->
                            blurCache = WeakReference(bitmap)
                            blurUnique = unique
                            blur_civ.setImageBitmap(bitmap)
                            visibility = View.VISIBLE
                        }
                    }
                }
            }
        } else {
            blur_civ.setImageBitmap(blurCache!!.get())
            visibility = View.VISIBLE
        }
    }

    fun hideBlur() {
        visibility = View.GONE
    }
}