package one.mixin.android.ui.wallet.alert.vo

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import one.mixin.android.R

enum class AlertAction {
    RESUME,
    PAUSE,
    EDIT,
    DELETE;

    @DrawableRes
    fun getIconResId(): Int {
        return when (this) {
            RESUME -> R.drawable.ic_action_resume
            PAUSE -> R.drawable.ic_action_pause
            EDIT -> R.drawable.ic_action_edit
            DELETE -> R.drawable.ic_action_delete
        }
    }

    @StringRes
    fun getStringResId(): Int {
        return when (this) {
            RESUME -> R.string.Resume
            PAUSE -> R.string.Pause
            EDIT -> R.string.Edit
            DELETE -> R.string.Delete
        }
    }
}
