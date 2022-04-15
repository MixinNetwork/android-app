package one.mixin.android.ui.home.circle

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.CircleConversationPayload
import one.mixin.android.api.request.CircleConversationRequest
import one.mixin.android.databinding.FragmentConversationCircleEditBinding
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.textColor
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.forward.ForwardAdapter
import one.mixin.android.ui.home.ConversationListViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.CircleConversationAction
import one.mixin.android.vo.ConversationCircleItem
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.isContactConversation

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class ConversationCircleEditFragment : BaseFragment() {
    companion object {
        const val TAG = "ConversationCircleEditFragment"

        const val ARGS_CIRCLE = "args_circle"

        private const val CIRCLE_CONVERSATION_LIMIT = 5

        fun newInstance(
            circle: ConversationCircleItem
        ) = ConversationCircleEditFragment().apply {
            arguments = bundleOf(
                ARGS_CIRCLE to circle
            )
        }
    }

    private val chatViewModel by viewModels<ConversationListViewModel>()

    private val circle: ConversationCircleItem by lazy {
        requireArguments().getParcelable(ARGS_CIRCLE)!!
    }

    private val adapter = ForwardAdapter(true)

    private val selectAdapter: ConversationCircleSelectAdapter by lazy {
        ConversationCircleSelectAdapter { item ->
            adapter.selectItem.remove(item)
            selectAdapter.checkedItems.remove(item)
            selectAdapter.notifyDataSetChanged()
            updateTitleText(adapter.selectItem.size)
        }
    }

    private var oldCircleConversationPayloadSet = mutableSetOf<CircleConversationPayload>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_conversation_circle_edit, container, false)

    private val binding by viewBinding(FragmentConversationCircleEditBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        binding.titleView.rightAnimator.setOnClickListener {
            save()
        }
        updateTitleText(circle.count)

        binding.selectRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.selectRv.adapter = selectAdapter
        binding.conversationRv.adapter = adapter
        binding.conversationRv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        adapter.setForwardListener(
            object : ForwardAdapter.ForwardListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onUserItemClick(user: User) {
                    lifecycleScope.launch {
                        if (adapter.selectItem.contains(user)) {
                            adapter.selectItem.remove(user)
                            selectAdapter.checkedItems.remove(user)
                        } else {
                            val count = chatViewModel.getCircleConversationCount(
                                generateConversationId(
                                    Session.getAccountId()!!, user.userId
                                )
                            )
                            if (count >= CIRCLE_CONVERSATION_LIMIT) {
                                toast(R.string.circle_limit)
                                return@launch
                            }
                            adapter.selectItem.add(user)
                            selectAdapter.checkedItems.add(user)
                        }
                        adapter.notifyDataSetChanged()
                        selectAdapter.notifyDataSetChanged()
                        binding.selectRv.layoutManager?.scrollToPosition(selectAdapter.checkedItems.size - 1)
                        updateTitleText(adapter.selectItem.size)
                    }
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onConversationClick(item: ConversationMinimal) {
                    lifecycleScope.launch {
                        if (adapter.selectItem.contains(item)) {
                            adapter.selectItem.remove(item)
                            selectAdapter.checkedItems.remove(item)
                        } else {
                            val count = chatViewModel.getCircleConversationCount(item.conversationId)
                            if (count >= CIRCLE_CONVERSATION_LIMIT) {
                                toast(R.string.circle_limit)
                                return@launch
                            }
                            adapter.selectItem.add(item)
                            selectAdapter.checkedItems.add(item)
                        }
                        adapter.notifyDataSetChanged()
                        selectAdapter.notifyDataSetChanged()
                        binding.selectRv.layoutManager?.scrollToPosition(selectAdapter.checkedItems.size - 1)
                        updateTitleText(adapter.selectItem.size)
                    }
                }
            }
        )
        binding.searchEt.et.addTextChangedListener(mWatcher)
        loadData()
    }

    private fun updateTitleText(size: Int) {
        if (!hasChanged()) {
            binding.titleView.rightTv.textColor = resources.getColor(R.color.text_gray, null)
            binding.titleView.rightAnimator.isEnabled = false
        } else {
            binding.titleView.rightTv.textColor = resources.getColor(R.color.colorBlue, null)
            binding.titleView.rightAnimator.isEnabled = true
        }
        binding.titleView.setSubTitle(circle.name, requireContext().resources.getQuantityString(R.plurals.circle_subtitle, size, size))
    }

    private fun hasChanged(): Boolean {
        return oldCircleConversationPayloadSet.size != adapter.selectItem.size || oldCircleConversationPayloadSet.map { it.conversationId }.sorted() !=
            adapter.selectItem.map { item ->
                when (item) {
                    is User -> {
                        generateConversationId(Session.getAccountId()!!, item.userId)
                    }
                    is ConversationMinimal -> {
                        item.conversationId
                    }
                    else -> {
                        ""
                    }
                }
            }.sorted()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadData() = lifecycleScope.launch {
        val conversations = chatViewModel.successConversationList()
        adapter.sourceConversations = conversations
        val conversationItems = chatViewModel.findConversationItemByCircleId(circle.circleId)
        val circleConversations = chatViewModel.findCircleConversationByCircleId(circle.circleId)
        val inCircleContactId = mutableListOf<String>()
        circleConversations.forEach { cc ->
            oldCircleConversationPayloadSet.add(
                CircleConversationPayload(
                    cc.conversationId,
                    if (cc.userId.isNullOrEmpty()) {
                        null
                    } else {
                        cc.userId
                    }
                )
            )
            if (cc.userId != null) {
                inCircleContactId.add(cc.userId)
            }
        }
        adapter.selectItem.clear()
        adapter.selectItem.addAll(conversationItems)
        selectAdapter.checkedItems.clear()
        selectAdapter.checkedItems.addAll(conversationItems)
        val set = ArraySet<String>()
        conversations.forEach { item ->
            if (item.isContactConversation()) {
                set.add(item.ownerId)
            }
        }

        val friends = mutableListOf<User>()
        val bots = mutableListOf<User>()
        val inCircleUsers = mutableListOf<User>()
        chatViewModel.getFriends().filter { item ->
            !set.contains(item.userId)
        }.forEach {
            if (inCircleContactId.contains(it.userId)) {
                inCircleUsers.add(it)
            }
            if (it.isBot()) {
                bots.add(it)
            } else {
                friends.add(it)
            }
        }
        adapter.sourceFriends = friends
        adapter.sourceBots = bots
        adapter.selectItem.addAll(inCircleUsers)
        selectAdapter.checkedItems.addAll(inCircleUsers)
        selectAdapter.notifyDataSetChanged()
        adapter.changeData()
        updateTitleText(adapter.selectItem.size)
    }

    private fun save() = lifecycleScope.launch {
        val dialog = indeterminateProgressDialog(
            message = R.string.pb_dialog_message,
            title = R.string.Saving
        ).apply {
            setCancelable(false)
        }
        dialog.show()
        binding.searchEt.hideKeyboard()

        val conversationRequests = mutableSetOf<CircleConversationPayload>()
        adapter.selectItem.forEach { item ->
            if (item is User) {
                conversationRequests.add(
                    CircleConversationPayload(
                        generateConversationId(Session.getAccountId()!!, item.userId),
                        item.userId
                    )
                )
            } else if (item is ConversationMinimal) {
                conversationRequests.add(
                    CircleConversationPayload(
                        item.conversationId,
                        if (item.isContactConversation()) item.ownerId else null
                    )
                )
            }
        }
        val safeSet = oldCircleConversationPayloadSet.intersect(conversationRequests)
        val removeSet = oldCircleConversationPayloadSet.subtract(safeSet)
        val addSet = conversationRequests.subtract(safeSet)
        if (addSet.isEmpty() && removeSet.isEmpty()) parentFragmentManager.popBackStackImmediate()
        val request = mutableListOf<CircleConversationRequest>().apply {
            addAll(addSet.map { CircleConversationRequest(it.conversationId, it.userId, CircleConversationAction.ADD.name) })
            addAll(removeSet.map { CircleConversationRequest(it.conversationId, it.userId, CircleConversationAction.REMOVE.name) })
        }

        handleMixinResponse(
            invokeNetwork = {
                chatViewModel.updateCircleConversations(circle.circleId, request)
            },
            successBlock = {
                if (it.isSuccess) {
                    chatViewModel.saveCircle(circle.circleId, it.data, removeSet)
                }
                dialog.dismiss()
                parentFragmentManager.popBackStackImmediate()
            },
            exceptionBlock = {
                dialog.dismiss()
                return@handleMixinResponse false
            },
            failureBlock = {
                dialog.dismiss()
                return@handleMixinResponse false
            }
        )
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            adapter.keyword = s
            adapter.changeData()
        }
    }
}
