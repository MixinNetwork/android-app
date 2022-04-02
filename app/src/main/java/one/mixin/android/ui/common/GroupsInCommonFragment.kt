package one.mixin.android.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentGroupsInCommonBinding
import one.mixin.android.databinding.ItemGroupsInCommonBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.GroupMinimal

@AndroidEntryPoint
class GroupsInCommonFragment : BaseFragment(R.layout.fragment_groups_in_common) {
    companion object {
        const val TAG = "GroupsInCommonFragment"

        fun newInstance(userId: String) = GroupsInCommonFragment().withArgs {
            putString(ARGS_USER_ID, userId)
        }
    }

    private val viewModel by viewModels<BottomSheetViewModel>()
    private val binding by viewBinding(FragmentGroupsInCommonBinding::bind)

    private lateinit var userId: String

    private val groupAdapter = GroupAdapter { groupMinimal ->
        requireActivity().supportFragmentManager.popBackStackImmediate()
        ConversationActivity.show(requireContext(), groupMinimal.conversationId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId = requireNotNull(requireArguments().getString(ARGS_USER_ID))
        binding.apply {
            titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
            groupRv.adapter = groupAdapter
        }

        loadData()
    }

    private fun loadData() = lifecycleScope.launch {
        val common = viewModel.findSameConversations(requireNotNull(Session.getAccountId()), userId)
        groupAdapter.submitList(common)
        binding.va.displayedChild = if (common.isEmpty()) 0 else 1
    }
}

class GroupAdapter(private val onItemClick: (GroupMinimal) -> Unit) : ListAdapter<GroupMinimal, GroupHolder>(GroupMinimal.DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupHolder =
        GroupHolder(ItemGroupsInCommonBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: GroupHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, onItemClick) }
    }
}

class GroupHolder(val binding: ItemGroupsInCommonBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: GroupMinimal, onItemClick: (GroupMinimal) -> Unit) {
        binding.apply {
            icon.setGroup(item.groupIconUrl)
            name.text = item.groupName
            memberCount.text = root.context.resources.getQuantityString(R.plurals.group_participants_count, item.memberCount, item.memberCount)
            root.setOnClickListener { onItemClick(item) }
        }
    }
}
