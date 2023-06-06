package one.mixin.android.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentConversationBinding
import one.mixin.android.databinding.FragmentGroupBinding
import one.mixin.android.databinding.FragmentSelectDateBinding
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class SelectDateFragment  : BaseFragment() {
    companion object {
        const val TAG = "SelectDateFragment"

        fun newInstance() = SelectDateFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? =
        inflater.inflate(R.layout.fragment_select_date, container, false)

    private val binding by viewBinding(FragmentSelectDateBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }
}