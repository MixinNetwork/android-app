package one.mixin.android.ui.group

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_group.*
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.job.ConversationJob.Companion.TYPE_ADD
import one.mixin.android.job.ConversationJob.Companion.TYPE_CREATE
import one.mixin.android.job.ConversationJob.Companion.TYPE_REMOVE
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.group.adapter.GroupFriendAdapter
import one.mixin.android.ui.group.adapter.GroupSelectAdapter
import one.mixin.android.vo.User
import org.jetbrains.anko.textColor

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.leftIb.setOnClickListener {
            activity?.onBackPressed()
        }
        if (from == TYPE_ADD || from == TYPE_REMOVE) {
            title_view.rightTv.text = getString(R.string.done)
            updateTitle(alreadyUsers?.size ?: 0)
        } else if (from == TYPE_CREATE) {
            updateTitle(0)
        }
        select_rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        select_rv.adapter = groupAdapter
        groupAdapter.checkedUsers = checkedUsers
        title_view.rightAnimator.setOnClickListener {
            search_et.hideKeyboard()
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
        title_view.rightAnimator.isEnabled = false
        groupFriendAdapter.setGroupFriendListener(mGroupFriendListener)
        alreadyUsers?.let {
            val alreadyUserIds = mutableListOf<String>()
            it.mapTo(alreadyUserIds) { it.userId }
            groupFriendAdapter.alreadyUserIds = alreadyUserIds
        }
        group_rv.adapter = groupFriendAdapter
        group_rv.addItemDecoration(StickyRecyclerHeadersDecoration(groupFriendAdapter))

        if (from == TYPE_ADD || from == TYPE_CREATE) {
            groupViewModel.getFriends().observe(
                viewLifecycleOwner,
                Observer {
                    users = it
                    filterAndSet(search_et.text.toString(), it)
                }
            )
        } else {
            users = alreadyUsers
            filterAndSet(search_et.text.toString(), alreadyUsers)
        }
        search_et.addTextChangedListener(mWatcher)

        search_et.isFocusableInTouchMode = false
        search_et.isFocusable = false
        search_et.post {
            search_et?.let {
                it.isFocusableInTouchMode = true
                it.isFocusable = true
            }
        }
    }

    private fun filterAndSet(keyword: String, userList: List<User>?) {
        groupFriendAdapter.setData(
            userList?.filter {
                it.fullName!!.contains(keyword, true) ||
                    it.identityNumber.contains(keyword, true)
            }?.sortedByDescending { it.fullName == keyword || it.identityNumber == keyword },
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
                if (from == TYPE_ADD) R.string.group_adding else R.string.group_removing
            dialog = indeterminateProgressDialog(
                message = R.string.pb_dialog_message,
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
        title_view.setSubTitle(
            when (from) {
                TYPE_REMOVE -> getString(R.string.group_info_remove_member)
                else -> getString(R.string.group_add)
            },
            "$size/$MAX_USER"
        )
    }

    private val mGroupFriendListener = object : GroupFriendAdapter.GroupFriendListener {
        override fun onItemClick(user: User, checked: Boolean) {
            if (checked) {
                checkedUsers.add(user)
                search_et?.text?.clear()
            } else {
                checkedUsers.remove(user)
            }
            val existCount = if (alreadyUsers == null) 0 else alreadyUsers!!.size
            updateTitle(
                if (from == TYPE_ADD || from == TYPE_CREATE)
                    checkedUsers.size + existCount else existCount - checkedUsers.size
            )
            groupAdapter.notifyDataSetChanged()
            select_rv.layoutManager?.scrollToPosition(checkedUsers.size - 1)
            if (checkedUsers.isEmpty()) {
                title_view.rightTv.textColor = resources.getColor(R.color.text_gray, null)
                title_view.rightAnimator.isEnabled = false
            } else {
                title_view.rightTv.textColor = resources.getColor(R.color.colorBlue, null)
                title_view.rightAnimator.isEnabled = true
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
