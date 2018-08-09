package android.support.v4.app

open class MixinDialogFragment : DialogFragment() {

    override fun showNow(manager: FragmentManager, tag: String) {
        mDismissed = false
        mShownByMe = true
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitNowAllowingStateLoss()
    }
}