package one.mixin.android.ui.media

import one.mixin.android.ui.common.BaseViewModelFragment

class AudioFragment : BaseViewModelFragment<SharedMediaViewModel>() {
    companion object {
        const val TAG = "AudioFragment"

        fun newInstance() = AudioFragment()
    }

    override fun getModelClass() = SharedMediaViewModel::class.java
}