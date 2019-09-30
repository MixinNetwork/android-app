package one.mixin.android.ui.media

import one.mixin.android.ui.common.BaseViewModelFragment

class FileFragment : BaseViewModelFragment<SharedMediaViewModel>() {
    companion object {
        const val TAG = "FileFragment"

        fun newInstance() = FileFragment()
    }

    override fun getModelClass() = SharedMediaViewModel::class.java
}