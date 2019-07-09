package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModel
import kotlinx.android.synthetic.main.fragment_verification.*
import one.mixin.android.Constants
import one.mixin.android.util.ErrorHandler

abstract class FabLoadingFragment<VH : ViewModel> : BaseViewModelFragment<VH>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        back_iv.setOnClickListener { activity?.onBackPressed() }
        verification_keyboard.setKeyboardKeys(Constants.KEYS)
        verification_cover.isClickable = true
    }

    protected fun handleError(t: Throwable) {
        verification_next_fab.hide()
        verification_cover.visibility = View.GONE
        ErrorHandler.handleError(t)
    }

    protected fun showLoading() {
        verification_next_fab.visibility = View.VISIBLE
        verification_next_fab.show()
        verification_cover.visibility = View.VISIBLE
    }

    protected open fun hideLoading() {
        verification_next_fab.hide()
        verification_next_fab.visibility = View.GONE
        verification_cover.visibility = View.GONE
    }
}