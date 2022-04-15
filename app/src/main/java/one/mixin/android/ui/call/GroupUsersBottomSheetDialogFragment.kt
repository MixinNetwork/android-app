package one.mixin.android.ui.call

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentGroupUsersBottomSheetBinding
import one.mixin.android.extension.alert
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendMessageJob
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.createCallMessage
import one.mixin.android.webrtc.publish
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView
import java.util.UUID
import javax.inject.Inject

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class GroupUsersBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "GroupUsersBottomSheetDialogFragment"
        const val GROUP_VOICE_MAX_COUNT = 256

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
            binding.actionIv.isVisible = selectAdapter.checkedUsers.isNotEmpty()
            selectAdapter.notifyDataSetChanged()
            groupUserAdapter.removeUser(it)
        }
    }

    private val binding by viewBinding(FragmentGroupUsersBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val view = binding.root
        context?.let { c ->
            val topOffset = c.statusBarHeight() + c.appCompatActionBarHeight()
            view.heightOffset = topOffset
        }
        contentView = view
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
            setCustomViewHeight(requireContext().realSize().y * 2 / 3)
        }
        val inGroupCallUsers = callState.getUsers(conversationId)

        contentView.apply {
            binding.closeIv.setOnClickListener { dismiss() }
            binding.searchEt.listener = object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    filter(s.toString(), users)
                }

                override fun onSearch() {
                }
            }
            binding.searchEt.setHint(getString(R.string.contact_search_hint))

            binding.selectRv.layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.selectRv.adapter = selectAdapter
            binding.userRv.layoutManager = LinearLayoutManager(requireContext())
            binding.userRv.adapter = groupUserAdapter

            if (callState.isGroupCall() && !inGroupCallUsers.isNullOrEmpty()) {
                binding.actionIv.setImageResource(R.drawable.ic_check)
            } else {
                binding.actionIv.setImageResource(R.drawable.ic_invite_call)
            }
            binding.actionIv.setOnClickListener {
                val users = arrayListOf<String>()
                checkedUsers.mapTo(users) { it.userId }
                val message = createCallMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    "",
                    MessageCategory.KRAKEN_INVITE.name,
                    "",
                    nowInUtc(),
                    MessageStatus.SENDING.name
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
                binding.actionIv.isVisible = checkedUsers.isNotEmpty()
                binding.selectRv.layoutManager?.scrollToPosition(checkedUsers.size - 1)
            }

            override fun onFull() {
                alert(getString(R.string.call_group_full, GROUP_VOICE_MAX_COUNT))
                    .setPositiveButton(R.string.OK) { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
        }

        selectAdapter.checkedUsers = checkedUsers

        lifecycleScope.launch {
            val users = bottomViewModel.getParticipantsWithoutBot(conversationId)
            this@GroupUsersBottomSheetDialogFragment.users = users
            filter(binding.searchEt.et.text.toString().trim(), users)
        }
    }

    private fun filter(keyword: String, users: List<User>?) {
        groupUserAdapter.submitList(
            users?.filter {
                it.fullName!!.containsIgnoreCase(keyword) ||
                    it.identityNumber.containsIgnoreCase(keyword)
            }?.sortedByDescending { it.fullName.equalsIgnoreCase(keyword) || it.identityNumber.equalsIgnoreCase(keyword) }
        )
    }
}
