package one.mixin.android.ui.media.pager.overlay

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.supportsPie
import one.mixin.android.ui.player.FloatingPlayer
import one.mixin.android.ui.player.MusicService
import one.mixin.android.ui.web.FloatingWebClip
import one.mixin.android.ui.web.clips
import one.mixin.android.ui.web.refresh
import one.mixin.android.util.SystemUIManager

/**
 * Fullscreen transparent edge-to-edge overlay window hosted inside the current Activity
 * without launching a new Activity. Keeps host decorView padding untouched, isolates
 * status/navigation bar appearance, and renders above BottomSheetDialogFragment windows.
 */
class MediaOverlayDialog(
    val hostActivity: FragmentActivity,
    private val keepScreenOn: Boolean = false,
    private val hideFloatingViews: Boolean = true,
    private val onDispatchTouchEvent: ((MotionEvent) -> Unit)? = null,
    private val onBackRequest: () -> Unit,
) : ComponentDialog(hostActivity, R.style.AppTheme_MediaOverlay) {

    private var systemBarsVisible: Boolean = true
    private var hostLifecycleObserver: DefaultLifecycleObserver? = null
    private var floatingHidden = false

    init {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackRequest()
                }
            },
        )
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        onDispatchTouchEvent?.invoke(ev)
        return super.dispatchTouchEvent(ev)
    }

    @Suppress("DEPRECATION")
    fun configureOverlayWindow() {
        val w = window ?: return
        w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        w.setWindowAnimations(0)
        w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (keepScreenOn) {
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        w.statusBarColor = Color.TRANSPARENT
        w.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            w.isStatusBarContrastEnforced = false
            w.isNavigationBarContrastEnforced = false
        }
        WindowCompat.setDecorFitsSystemWindows(w, false)
        supportsPie {
            val lp = w.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            w.attributes = lp
        }
        SystemUIManager.lightUI(w, false)
    }

    override fun show() {
        if (hostActivity.isFinishing || hostActivity.isDestroyed) return
        hostActivity.hideKeyboard()
        configureOverlayWindow()
        hideFloatingIfNeeded()
        attachHostLifecycle()
        super.show()
        // Re-apply layout after show so OEM themes cannot shrink the dialog window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun setSystemBarsVisible(visible: Boolean) {
        if (systemBarsVisible == visible) return
        systemBarsVisible = visible
        val w = window ?: return
        if (visible) {
            SystemUIManager.showSystemUI(w)
            SystemUIManager.lightUI(w, false)
        } else {
            SystemUIManager.hideSystemUI(w)
        }
    }

    fun restoreSystemBarsForDismiss() {
        val w = window ?: return
        if (!systemBarsVisible) {
            systemBarsVisible = true
            SystemUIManager.showSystemUI(w)
        }
    }

    override fun dismiss() {
        detachHostLifecycle()
        restoreFloatingIfNeeded()
        if (isShowing) {
            super.dismiss()
        }
    }

    private fun hideFloatingIfNeeded() {
        if (!hideFloatingViews || floatingHidden) return
        floatingHidden = true
        val night = hostActivity.isNightMode()
        FloatingWebClip.getInstance(night).hide()
        FloatingPlayer.getInstance(night).hide()
    }

    private fun restoreFloatingIfNeeded() {
        if (!floatingHidden) return
        floatingHidden = false
        if (hostActivity.isFinishing || hostActivity.isDestroyed) return
        val night = hostActivity.isNightMode()
        if (clips.isNotEmpty()) {
            refresh()
        }
        if (MusicService.isRunning(hostActivity)) {
            FloatingPlayer.getInstance(night).show()
        }
    }

    private fun attachHostLifecycle() {
        detachHostLifecycle()
        val observer =
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    if (isShowing) {
                        super@MediaOverlayDialog.dismiss()
                    }
                    detachHostLifecycle()
                }
            }
        hostLifecycleObserver = observer
        hostActivity.lifecycle.addObserver(observer)
    }

    private fun detachHostLifecycle() {
        hostLifecycleObserver?.let {
            hostActivity.lifecycle.removeObserver(it)
        }
        hostLifecycleObserver = null
    }
}
