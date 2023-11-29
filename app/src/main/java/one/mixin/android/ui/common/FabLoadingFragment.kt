package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import one.mixin.android.R
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.util.ErrorHandler
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.fabprogresscircle.FABProgressCircle

abstract class FabLoadingFragment : BaseFragment {
    constructor() : super()
    constructor(
        @LayoutRes contentLayoutId: Int,
    ) : super(contentLayoutId)

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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        backIv.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        verificationKeyboard.initPinKeys()
        verificationCover.isClickable = true
    }

    protected fun handleError(t: Throwable) {
        if (viewDestroyed()) return

        verificationNextFab.hide()
        verificationCover.visibility = View.GONE
        ErrorHandler.handleError(t)
    }

    protected fun showLoading() {
        if (viewDestroyed()) return

        verificationNextFab.visibility = View.VISIBLE
        verificationNextFab.show()
        verificationCover.visibility = View.VISIBLE
    }

    protected open fun hideLoading() {
        if (viewDestroyed()) return

        verificationNextFab.hide()
        verificationNextFab.visibility = View.GONE
        verificationCover.visibility = View.GONE
    }
}
