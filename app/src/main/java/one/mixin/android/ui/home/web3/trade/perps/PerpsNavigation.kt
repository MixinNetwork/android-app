package one.mixin.android.ui.home.web3.trade.perps

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import one.mixin.android.R

internal fun FragmentManager.navigateToPerpsRoute(
    fragment: Fragment,
    tag: String,
    containerId: Int,
    animate: Boolean = true,
) {
    if (findFragmentByTag(tag) != null) {
        popBackStack(tag, 0)
        return
    }
    beginTransaction()
        .apply {
            if (animate) {
                setCustomAnimations(
                    R.anim.slide_in_right,
                    0,
                    0,
                    R.anim.slide_out_right,
                )
            }
        }
        .add(containerId, fragment, tag)
        .addToBackStack(tag)
        .commit()
}
