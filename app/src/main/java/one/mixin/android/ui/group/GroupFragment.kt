package one.mixin.android.ui.group

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentGroupBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.textColor
import one.mixin.android.job.ConversationJob.Companion.TYPE_ADD
import one.mixin.android.job.ConversationJob.Companion.TYPE_CREATE
import one.mixin.android.job.ConversationJob.Companion.TYPE_REMOVE
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.group.adapter.GroupFriendAdapter
import one.mixin.android.ui.group.adapter.GroupSelectAdapter
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.User

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class GroupFragment : BaseFragment() {

    companion object {
        const val TAG = "GroupFragment"

        const val ARGS_FROM = "args_from"
        const val ARGS_ALREADY_USERS = "args_already_users"

        const val MAX_USER = 256

        fun newInstance(
            from: Int? = 0,
            alreadyUsers: ArrayList<User>? = null,
            conversationId: String? = null
        ): GroupFragment {
            val f = GroupFragment()
            val b = Bundle()
            from?.let {
                b.putInt(ARGS_FROM, it)
            }
            alreadyUsers?.let {
                b.putParcelableArrayList(ARGS_ALREADY_USERS, it)
            }
            conversationId?.let {
                b.putString(ARGS_CONVERSATION_ID, conversationId)
            }
            f.arguments = b
            return f
        }
    }

    private val groupViewModel by viewModels<GroupViewModel>()

    private val from: Int by lazy {
        requireArguments().getInt(ARGS_FROM)
    }

    private val alreadyUsers: ArrayList<User>? by lazy {
        requireArguments().getParcelableArrayList<User>(ARGS_ALREADY_USERS)
    }

    private val conversationId: String? by lazy {
        requireArguments().getString(ARGS_CONVERSATION_ID)
    }

    private val groupFriendAdapter: GroupFriendAdapter by lazy {
        GroupFriendAdapter().apply { isAdd = from == TYPE_ADD }
    }

    private var users: List<User>? = null
    private var checkedUsers: MutableList<User> = mutableListOf()
    private var dialog: Dialog? = null

    private val groupAdapter: GroupSelectAdapter by lazy {
        GroupSelectAdapter { user ->
            checkedUsers.remove(user)
            groupFriendAdapter.clearUser(user)
            groupAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_group, container, false)

    private val binding by viewBinding(FragmentGroupBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            activity?.onBackPressed()
        }
        if (from == TYPE_ADD || from == TYPE_REMOVE) {
            binding.titleView.rightTv.text = getString(R.string.Done)
            updateTitle(alreadyUsers?.size ?: 0)
        } else if (from == TYPE_CREATE) {
            updateTitle(0)
        }
        binding.selectRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.selectRv.adapter = groupAdapter
        groupAdapter.checkedUsers = checkedUsers
        binding.titleView.rightAnimator.setOnClickListener {
            binding.searchEt.hideKeyboard()
            if (from == TYPE_ADD || from == TYPE_REMOVE) {
                handleAddOrRemove()
            } else {
                activity?.addFragment(
                    this@GroupFragment,
                    NewGroupFragment.newInstance(ArrayList(checkedUsers)),
                    NewGroupFragment.TAG
                )
            }
        }
        binding.titleView.rightAnimator.isEnabled = false
        groupFriendAdapter.setGroupFriendListener(mGroupFriendListener)
        alreadyUsers?.let {
            val alreadyUserIds = mutableListOf<String>()
            it.mapTo(alreadyUserIds) { it.userId }
            groupFriendAdapter.alreadyUserIds = alreadyUserIds
        }
        binding.groupRv.adapter = groupFriendAdapter
        binding.groupRv.addItemDecoration(StickyRecyclerHeadersDecoration(groupFriendAdapter))

        if (from == TYPE_ADD || from == TYPE_CREATE) {
            groupViewModel.getFriends().observe(
                viewLifecycleOwner
            ) {
                users = it
                filterAndSet(binding.searchEt.text.toString(), it)
            }
        } else {
            users = alreadyUsers
            filterAndSet(binding.searchEt.text.toString(), alreadyUsers)
        }
        binding.searchEt.addTextChangedListener(mWatcher)

        binding.searchEt.isFocusableInTouchMode = false
        binding.searchEt.isFocusable = false
        binding.searchEt.post {
            binding.searchEt.let {
                it.isFocusableInTouchMode = true
                it.isFocusable = true
            }
        }
    }

    private fun filterAndSet(keyword: String, userList: List<User>?) {
        groupFriendAdapter.setData(
            userList?.filter {
                it.fullName!!.containsIgnoreCase(keyword) ||
                    it.identityNumber.containsIgnoreCase(keyword)
            }?.sortedByDescending { it.fullName.equalsIgnoreCase(keyword) || it.identityNumber.equalsIgnoreCase(keyword) },
            keyword.isEmpty()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.dismiss()
    }

    private fun handleAddOrRemove() = lifecycleScope.launch {
        if (dialog == null) {
            val title =
                if (from == TYPE_ADD) R.string.Adding else R.string.Removing
            dialog = indeterminateProgressDialog(
                message = R.string.Please_wait_a_bit,
                title = title
            ).apply {
                setCancelable(false)
            }
        }
        dialog?.show()
        val result = groupViewModel.modifyMember(conversationId!!, checkedUsers, from)
        dialog?.dismiss()
        if (result) {
            if (isAdded) {
                activity?.supportFragmentManager?.popBackStackImmediate()
            } else {
                activity?.supportFragmentManager?.popBackStack()
            }
        }
    }

    private fun updateTitle(size: Int) {
        binding.titleView.setSubTitle(
            when (from) {
                TYPE_REMOVE -> getString(R.string.Remove_Participants)
                else -> getString(R.string.Add_participants)
            },
            "$size/$MAX_USER"
        )
    }

    private val mGroupFriendListener = object : GroupFriendAdapter.GroupFriendListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onItemClick(user: User, checked: Boolean) {
            if (checked) {
                checkedUsers.add(user)
                binding.searchEt.text?.clear()
            } else {
                checkedUsers.remove(user)
            }
            val existCount = if (alreadyUsers == null) 0 else alreadyUsers!!.size
            updateTitle(
                if (from == TYPE_ADD || from == TYPE_CREATE)
                    checkedUsers.size + existCount else existCount - checkedUsers.size
            )
            groupAdapter.notifyDataSetChanged()
            binding.selectRv.layoutManager?.scrollToPosition(checkedUsers.size - 1)
            if (checkedUsers.isEmpty()) {
                binding.titleView.rightTv.textColor = resources.getColor(R.color.text_gray, null)
                binding.titleView.rightAnimator.isEnabled = false
            } else {
                binding.titleView.rightTv.textColor = resources.getColor(R.color.colorBlue, null)
                binding.titleView.rightAnimator.isEnabled = true
            }
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            val keyword = s.toString().trim()
            filterAndSet(keyword, users)
        }
    }
}
