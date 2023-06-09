package one.mixin.android.ui.transfer

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSelectConverstionBinding
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
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
    private val chatViewModel by viewModels<ConversationViewModel>()

    private val adapter by lazy {
        SelectAdapter({ isAll ->
            if (isAll) {
                binding.selectTv.setText(R.string.Deselect_all)
            } else {
                binding.selectTv.setText(R.string.Select_all)
            }
        }, { count ->
            binding.showTv.text = getString(R.string.Show_Selected, count)
            binding.showTv.isEnabled = count > 0
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.recyclerView.adapter = adapter
        binding.searchEt.addTextChangedListener(mWatcher)
        binding.showTv.text = getString(R.string.Show_Selected, 0)
        binding.showTv.isEnabled = false
        binding.titleView.rightTv.setOnClickListener {
            callback?.invoke(
                if (adapter.isAll) {
                    null
                } else {
                    adapter.selectItem
                },
            )
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        lifecycleScope.launch {
            adapter.conversations = chatViewModel.successConversationList()
            adapter.notifyDataSetChanged()
        }
        binding.selectTv.setOnClickListener {
            adapter.toggle()
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            adapter.keyword = s
        }
    }

    var callback: ((Set<String>?) -> Unit)? = null
}
