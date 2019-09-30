package one.mixin.android.ui.media

import one.mixin.android.ui.common.BaseViewModelFragment

class LinkFragment : BaseViewModelFragment<SharedMediaViewModel>() {
    companion object {
        const val TAG = "LinkFragment"

        fun newInstance() = LinkFragment()
    }

    override fun getModelClass() = SharedMediaViewModel::class.java
}