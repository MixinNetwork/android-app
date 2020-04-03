package one.mixin.android.ui.home.circle

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_conversation_circle_edit.*
import kotlinx.android.synthetic.main.fragment_conversation_circle_edit.search_et
import kotlinx.android.synthetic.main.fragment_group.select_rv
import kotlinx.android.synthetic.main.fragment_group.title_view
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.CircleConversationRequest
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.forward.ForwardAdapter
import one.mixin.android.ui.home.ConversationListViewModel
import one.mixin.android.util.Session
import one.mixin.android.vo.ConversationCircleItem
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import org.jetbrains.anko.textColor

class ConversationCircleEditFragment : BaseFragment() {
    companion object {
        const val TAG = "ConversationCircleEditFragment"

        const val ARGS_CIRCLE = "args_circle"

        fun newInstance(
            circle: ConversationCircleItem
        ) = ConversationCircleEditFragment().apply {
            arguments = bundleOf(
                ARGS_CIRCLE to circle
            )
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val chatViewModel: ConversationListViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ConversationListViewModel::class.java)
    }

    private val circle: ConversationCircleItem by lazy {
        requireArguments().getParcelable<ConversationCircleItem>(ARGS_CIRCLE)!!
    }

    private val adapter = ForwardAdapter()

    private val selectAdapter: ConversationCircleSelectAdapter by lazy {
        ConversationCircleSelectAdapter { item ->
            adapter.selectItem.remove(item)
            selectAdapter.checkedItems.remove(item)
            selectAdapter.notifyDataSetChanged()
        }
    }

    private var oldCircleConversationRequestSet = mutableSetOf<CircleConversationRequest>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_conversation_circle_edit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }
        title_view.right_animator.setOnClickListener {
            save()
        }
        updateTitleText(circle.count)

        select_rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        select_rv.adapter = selectAdapter
        conversation_rv.adapter = adapter
        conversation_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        adapter.setForwardListener(object : ForwardAdapter.ForwardListener {
            override fun onUserItemClick(user: User) {
                if (adapter.selectItem.contains(user)) {
                    adapter.selectItem.remove(user)
                    selectAdapter.checkedItems.remove(user)
                } else {
                    adapter.selectItem.add(user)
                    selectAdapter.checkedItems.add(user)
                }
                adapter.notifyDataSetChanged()
                selectAdapter.notifyDataSetChanged()
                select_rv.layoutManager?.scrollToPosition(selectAdapter.checkedItems.size - 1)
                updateTitleText(adapter.selectItem.size)
            }

            override fun onConversationItemClick(item: ConversationItem) {
                if (adapter.selectItem.contains(item)) {
                    adapter.selectItem.remove(item)
                    selectAdapter.checkedItems.remove(item)
                } else {
                    adapter.selectItem.add(item)
                    selectAdapter.checkedItems.add(item)
                }
                adapter.notifyDataSetChanged()
                selectAdapter.notifyDataSetChanged()
                select_rv.layoutManager?.scrollToPosition(selectAdapter.checkedItems.size - 1)
                updateTitleText(adapter.selectItem.size)
            }
        })
        search_et.addTextChangedListener(mWatcher)

        loadData()
    }

    override fun onBackPressed(): Boolean {
        parentFragmentManager.popBackStackImmediate()
        return super.onBackPressed()
    }

    private fun updateTitleText(size: Int) {
        if (adapter.selectItem.isEmpty()) {
            title_view.right_tv.textColor = resources.getColor(R.color.text_gray, null)
            title_view.right_animator.isEnabled = false
        } else {
            title_view.right_tv.textColor = resources.getColor(R.color.colorBlue, null)
            title_view.right_animator.isEnabled = true
        }
        title_view.setSubTitle(circle.name, getString(R.string.circle_subtitle, size))
    }

    private fun loadData() = lifecycleScope.launch {
        val conversations = chatViewModel.successConversationList()
        adapter.sourceConversations = conversations
        val conversationItems = chatViewModel.findConversationItemByCircleId(circle.circleId)
        val circleConversations = chatViewModel.findCircleConversationByCircleId(circle.circleId)
        val inCircleContactId = mutableListOf<String>()
        circleConversations.forEach { cc ->
            oldCircleConversationRequestSet.add(CircleConversationRequest(cc.conversationId, cc.contactId))
            if (cc.contactId != null) {
                inCircleContactId.add(cc.contactId)
            }
        }
        adapter.selectItem.clear()
        adapter.selectItem.addAll(conversationItems)
        selectAdapter.checkedItems.clear()
        selectAdapter.checkedItems.addAll(conversationItems)
        val set = ArraySet<String>()
        conversations.forEach { item ->
            if (item.isContact()) {
                set.add(item.ownerId)
            }
        }
        val list = chatViewModel.getFriends()
        val filteredFriends = if (list.isNotEmpty()) {
            list.filter { item ->
                !set.contains(item.userId)
            }
        } else {
            list
        }
        adapter.sourceFriends = filteredFriends
        val inCircleUsers = mutableListOf<User>()
        filteredFriends.forEach {
            if (inCircleContactId.contains(it.userId)) {
                inCircleUsers.add(it)
            }
        }
        adapter.selectItem.addAll(inCircleUsers)
        selectAdapter.checkedItems.addAll(inCircleUsers)
        selectAdapter.notifyDataSetChanged()
        adapter.changeData()
        updateTitleText(adapter.selectItem.size)
    }

    private fun save() = lifecycleScope.launch {
        val dialog = indeterminateProgressDialog(
            message = R.string.pb_dialog_message,
            title = R.string.saving
        ).apply {
            setCancelable(false)
        }
        dialog.show()

        val conversationRequests = mutableListOf<CircleConversationRequest>()
        adapter.selectItem.forEach { item ->
            if (item is User) {
                conversationRequests.add(
                    CircleConversationRequest(
                        generateConversationId(Session.getAccountId()!!, item.userId),
                        item.userId
                    )
                )
            } else if (item is ConversationItem) {
                conversationRequests.add(
                    CircleConversationRequest(item.conversationId)
                )
            }
        }
        handleMixinResponse(
            switchContext = Dispatchers.IO,
            invokeNetwork = {
                chatViewModel.updateCircleConversations(circle.circleId, conversationRequests)
            },
            successBlock = {
                val newConversationIds = mutableListOf<String>()
                conversationRequests.forEach { item ->
                    newConversationIds.add(item.conversationId)
                }
                val result = chatViewModel.saveCircle(oldCircleConversationRequestSet, conversationRequests, circle.circleId)
                dialog.dismiss()
                if (result) {
                    parentFragmentManager.popBackStackImmediate()
                }
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
