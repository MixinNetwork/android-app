package one.mixin.android.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSelectConverstionBinding
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class SelectConversationFragment : BaseFragment() {
    companion object {
        const val TAG = "SelectConversationFragment"

        fun newInstance() = SelectConversationFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? =
        inflater.inflate(R.layout.fragment_select_converstion, container, false)

    private val binding by viewBinding(FragmentSelectConverstionBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }
}
