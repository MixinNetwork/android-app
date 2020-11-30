package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import com.github.jorgecastilloprz.FABProgressCircle
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.Keyboard

abstract class FabLoadingFragment : BaseFragment {
    constructor() : super()
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    abstract fun getContentView(): View
    protected val _contentView get() = getContentView()

    protected val backIv: View by lazy {
        _contentView.findViewById(R.id.back_iv)
    }
    protected val verificationKeyboard: Keyboard by lazy {
        _contentView.findViewById(R.id.verification_keyboard)
    }
    protected val verificationCover: View by lazy {
        _contentView.findViewById(R.id.verification_cover)
    }
    protected val verificationNextFab: FABProgressCircle by lazy {
        _contentView.findViewById(R.id.verification_next_fab)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backIv.setOnClickListener { activity?.onBackPressed() }
        verificationKeyboard.setKeyboardKeys(Constants.KEYS)
        verificationCover.isClickable = true
    }

    protected fun handleError(t: Throwable) {
        if (!isAdded) return

        verificationNextFab.hide()
        verificationCover.visibility = View.GONE
        ErrorHandler.handleError(t)
    }

    protected fun showLoading() {
        if (!isAdded) return

        verificationNextFab.visibility = View.VISIBLE
        verificationNextFab.show()
        verificationCover.visibility = View.VISIBLE
    }

    protected open fun hideLoading() {
        if (!isAdded) return

        verificationNextFab.hide()
        verificationNextFab.visibility = View.GONE
        verificationCover.visibility = View.GONE
    }
}
