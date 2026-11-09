package one.mixin.android.ui.common

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.window.embedding.ActivityEmbeddingController
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.util.SystemUIManager
import java.lang.ref.WeakReference

// Transparent full-task host: on a split screen a dialog is clipped to its pane, so it opens here to center on the whole screen.
@AndroidEntryPoint
class TaskDialogActivity : BaseActivity() {
    companion object {
        private var pending: DialogFragment? = null
        private var pendingTag: String? = null
        private var originRef: WeakReference<FragmentActivity>? = null
        private var returning = false

        fun show(origin: FragmentActivity, dialog: DialogFragment, tag: String) {
            pending = dialog
            pendingTag = tag
            originRef = WeakReference(origin)
            origin.startActivity(Intent(origin, TaskDialogActivity::class.java))
        }

        // True once after a hosted dialog closes, so the opener's onResume does not show it again right away.
        fun consumeReturn(): Boolean {
            val value = returning
            returning = false
            return value
        }
    }

    val originActivity: FragmentActivity?
        get() = originRef?.get()

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Transparent

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Transparent

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSystemUi = true
        super.onCreate(savedInstanceState)
        SystemUIManager.setSafePadding(window, Color.TRANSPARENT)
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                    if (f !is DialogFragment || isChangingConfigurations) return
                    if (fm.fragments.none { it is DialogFragment && it !== f }) {
                        returning = true
                        finish()
                    }
                }
            },
            false,
        )
        if (savedInstanceState == null) {
            val dialog = pending
            val tag = pendingTag
            pending = null
            pendingTag = null
            if (dialog == null) {
                finish()
                return
            }
            dialog.show(supportFragmentManager, tag)
        }
    }
}

// The activity that opened a hosted dialog, so its actions keep acting on the original screen.
fun Fragment.taskActivity(): FragmentActivity? = (activity as? TaskDialogActivity)?.originActivity ?: activity

fun DialogFragment.showOnTask(manager: FragmentManager, tag: String) {
    val activity = manager.fragments.firstNotNullOfOrNull { it.activity }
    if (activity != null && activity !is TaskDialogActivity && ActivityEmbeddingController.getInstance(activity).isActivityEmbedded(activity)) {
        TaskDialogActivity.show(activity, this, tag)
    } else {
        show(manager, tag)
    }
}
