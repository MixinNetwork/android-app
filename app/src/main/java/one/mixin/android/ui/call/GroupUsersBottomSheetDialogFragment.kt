package one.mixin.android.ui.call

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_group_users_bottom_sheet.view.*
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.extension.alert
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendMessageJob
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.createCallMessage
import one.mixin.android.webrtc.publish
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.BottomSheetRelativeLayout
import one.mixin.android.widget.SearchView
import java.util.UUID
import javax.inject.Inject

class GroupUsersBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "GroupUsersBottomSheetDialogFragment"
        const val GROUP_VOICE_MAX_COUNT = 16

        fun newInstance(
            conversationId: String
        ) = GroupUsersBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_CONVERSATION_ID, conversationId)
            }
        }
    }

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var callState: CallStateLiveData

    private val conversationId: String by lazy {
        requireArguments().getString(ARGS_CONVERSATION_ID)!!
    }

    private var users: List<User>? = null
    private var checkedUsers: MutableList<User> = mutableListOf()

    private val groupUserAdapter = GroupUserAdapter()

    private val selectAdapter: UserSelectAdapter by lazy {
        UserSelectAdapter {
            checkedUsers.remove(it)
            selectAdapter.checkedUsers.remove(it)
            contentView.action_iv.isVisible = selectAdapter.checkedUsers.isNotEmpty()
            selectAdapter.notifyDataSetChanged()
            groupUserAdapter.removeUser(it)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val view = View.inflate(context, R.layout.fragment_group_users_bottom_sheet, null) as BottomSheetRelativeLayout
        context?.let { c ->
            val topOffset = c.statusBarHeight() + c.appCompatActionBarHeight()
            view.heightOffset = topOffset
        }
        contentView = view
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
            setCustomViewHeight((requireContext().realSize().y * .6f).toInt())
        }
        val inGroupCallUsers = callState.getUsers(conversationId)

        contentView.apply {
            close_iv.setOnClickListener { dismiss() }
            search_et.listener = object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    filter(s.toString(), users)
                }

                override fun onSearch() {
                }
            }
            search_et.setHint(getString(R.string.contact_search_hint))

            select_rv.layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL, false
            )
            select_rv.adapter = selectAdapter
            user_rv.layoutManager = LinearLayoutManager(requireContext())
            user_rv.adapter = groupUserAdapter

            if (callState.isGroupCall() && !inGroupCallUsers.isNullOrEmpty()) {
                action_iv.setImageResource(R.drawable.ic_check)
            } else {
                action_iv.setImageResource(R.drawable.ic_invite_call)
            }
            action_iv.setOnClickListener {
                val users = arrayListOf<String>()
                checkedUsers.mapTo(users) { it.userId }
                val message = createCallMessage(
                    UUID.randomUUID().toString(), conversationId,
                    "", MessageCategory.KRAKEN_INVITE.name, "", nowInUtc(), MessageStatus.SENDING.name
                )
                if (callState.isIdle()) {
                    publish(requireContext(), conversationId, users)
                } else {
                    callState.addPendingUsers(conversationId, users)
                }
                jobManager.addJobInBackground(SendMessageJob(message, recipientIds = users))
                dismiss()
            }
        }

        groupUserAdapter.alreadyUserIds = inGroupCallUsers
        groupUserAdapter.listener = object : GroupUserListener {
            override fun onItemClick(user: User, checked: Boolean) {
                if (checked) {
                    checkedUsers.add(user)
                } else {
                    checkedUsers.remove(user)
                }
                selectAdapter.notifyDataSetChanged()
                contentView.action_iv.isVisible = checkedUsers.isNotEmpty()
                contentView.select_rv.layoutManager?.scrollToPosition(checkedUsers.size - 1)
            }

            override fun onFull() {
                alert(getString(R.string.call_group_full, GROUP_VOICE_MAX_COUNT))
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
        }

        selectAdapter.checkedUsers = checkedUsers

        lifecycleScope.launch {
            val users = bottomViewModel.getParticipantsWithoutBot(conversationId)
            this@GroupUsersBottomSheetDialogFragment.users = users
            filter(contentView.search_et.text.toString().trim(), users)
        }
    }

    private fun filter(keyword: String, users: List<User>?) {
        groupUserAdapter.submitList(
            users?.filter {
                it.fullName!!.contains(keyword, true) ||
                    it.identityNumber.contains(keyword, true)
            }?.sortedByDescending { it.fullName == keyword || it.identityNumber == keyword }
        )
    }
}
