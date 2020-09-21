package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_verification.*
import one.mixin.android.Constants
import one.mixin.android.util.ErrorHandler

abstract class FabLoadingFragment : BaseFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        back_iv.setOnClickListener { activity?.onBackPressed() }
        verification_keyboard.setKeyboardKeys(Constants.KEYS)
        verification_cover.isClickable = true
    }

    protected fun handleError(t: Throwable) {
        if (!isAdded) return

        verification_next_fab.hide()
        verification_cover.visibility = View.GONE
        ErrorHandler.handleError(t)
    }

    protected fun showLoading() {
        if (!isAdded) return

        verification_next_fab.visibility = View.VISIBLE
        verification_next_fab.show()
        verification_cover.visibility = View.VISIBLE
    }

    protected open fun hideLoading() {
        if (!isAdded) return

        verification_next_fab.hide()
        verification_next_fab.visibility = View.GONE
        verification_cover.visibility = View.GONE
    }
}
