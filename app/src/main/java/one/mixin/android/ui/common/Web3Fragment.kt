package one.mixin.android.ui.common

import androidx.annotation.LayoutRes

abstract class Web3Fragment : BaseFragment {
    constructor() : super()
    constructor(
        @LayoutRes contentLayoutId: Int,
    ) : super(contentLayoutId)

    abstract fun updateUI()
}
