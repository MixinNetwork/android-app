package one.mixin.android.ui.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.round
import one.mixin.android.widget.SixLayout

class FloatingWebGroup {
    companion object {
        private val appContext by lazy {
            MixinApplication.appContext
        }

        @SuppressLint("StaticFieldLeak")
        private var Instance: FloatingWebGroup? = null

        fun getInstance(): FloatingWebGroup {
            var localInstance = Instance
            if (localInstance == null) {
                synchronized(FloatingWebGroup::class.java) {
                    localInstance = Instance
                    if (localInstance == null) {
                        localInstance = FloatingWebGroup()
                        Instance = localInstance
                    }
                }
            }
            return requireNotNull(localInstance)
        }
    }

    private val windowManager: WindowManager by lazy {
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private lateinit var windowView: ViewGroup
    private lateinit var container: ViewGroup
    private lateinit var windowLayoutParams: WindowManager.LayoutParams
    private var isShown = false
    fun show(activity: Activity) {
        if (isShown) return
        isShown = true
        if (!::windowView.isInitialized) {
            initWindowView(activity)
        }
        if (!::windowLayoutParams.isInitialized) {
            initWindowLayoutParams()
        }
        windowManager.addView(windowView, windowLayoutParams)
        reload(activity)
    }

    private fun initWindowView(activity: Activity) {
        windowView = object : FrameLayout(activity) {
            override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                if (event?.keyCode == KeyEvent.KEYCODE_BACK) {
                    collapse()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }
        container = SixLayout(activity)
        container.setPadding(24.dp, 24.dp, 24.dp, 24.dp)
        windowView.addView(container, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        val closeImage = ImageView(activity)
        closeImage.setImageResource(R.drawable.ic_action_navigation_close)
        closeImage.setOnClickListener {
            collapse()
        }
        container.setOnClickListener { collapse() }
        closeImage.setPadding(12.dp, 12.dp, 12.dp, 12.dp)
        windowView.addView(closeImage, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.END
        })
        windowView.setBackgroundColor(Color.parseColor("#AAFFFFFF"))
    }

    private fun initWindowLayoutParams() {
        windowLayoutParams = WindowManager.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        windowLayoutParams.x = 0
        windowLayoutParams.y = 0
        windowLayoutParams.format = PixelFormat.TRANSLUCENT
        windowLayoutParams.gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= 26) {
            windowLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            windowLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    }

    fun hide() {
        if (!isShown) return
        isShown = false
        windowManager.removeView(windowView)
    }

    private fun reload(activity: Activity) {
        container.removeAllViews()
        clips.forEach { (_, webClip) -> addItem(activity, webClip) }
    }

    fun addItem(activity: Activity, webClip: WebClip) {
        val thumbIv = object : AppCompatImageView(activity) {
            override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
                val matrix: Matrix = imageMatrix
                val scaleFactor = (r - l) / drawable.intrinsicWidth.toFloat()
                matrix.setScale(scaleFactor, scaleFactor, 0f, 0f)
                imageMatrix = matrix
                return super.setFrame(l, t, r, b)
            }
        }

        thumbIv.scaleType = ImageView.ScaleType.MATRIX
        thumbIv.setImageBitmap(webClip.thumb)
        thumbIv.round(8.dp)
        thumbIv.z = 1.dp.toFloat()
        // val itemContainer = FrameLayout(activity).apply {
        //     foregroundGravity = Gravity.CENTER
        //
        // }
        // itemContainer.addView(thumbIv, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        container.addView(thumbIv, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    fun deleteItem(index: Int) {
        windowView.removeViewAt(index)
    }
}