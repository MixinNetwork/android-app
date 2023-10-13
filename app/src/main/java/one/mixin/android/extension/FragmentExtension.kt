package one.mixin.android.extension

import androidx.fragment.app.Fragment

/**
 * Use this method to check whether fragment have destroyed view.
 *
 * This method should NOT be used for DialogFragment.
 */
fun Fragment.viewDestroyed(): Boolean =
    try {
        viewLifecycleOwner
        false
    } catch (e: IllegalStateException) {
        true
    }
