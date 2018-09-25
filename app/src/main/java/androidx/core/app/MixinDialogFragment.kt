package androidx.core.app

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

open class MixinDialogFragment : DialogFragment() {
    override fun showNow(manager: FragmentManager, tag: String) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitNowAllowingStateLoss()
    }
}