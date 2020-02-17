package one.mixin.android.ui.forward

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
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
import com.bugsnag.android.Bugsnag
import com.tbruyelle.rxpermissions2.RxPermissions
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import com.uber.autodispose.autoDispose
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_forward.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.ForwardEvent
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_FROM_CONVERSATION
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_MESSAGES
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_SHARE
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.User

class ForwardFragment : BaseFragment() {
    companion object {
        const val TAG = "ForwardFragment"

        fun newInstance(
            messages: ArrayList<ForwardMessage>,
            isShare: Boolean = false,
            fromConversation: Boolean = false
        ): ForwardFragment {
            val fragment = ForwardFragment()
            val b = bundleOf(
                ARGS_MESSAGES to messages,
                ARGS_SHARE to isShare,
                ARGS_FROM_CONVERSATION to fromConversation
            )
            fragment.arguments = b
            return fragment
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val chatViewModel: ConversationViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val adapter by lazy {
        ForwardAdapter()
    }

    private val messages: ArrayList<ForwardMessage>? by lazy {
        arguments!!.getParcelableArrayList<ForwardMessage>(ARGS_MESSAGES)
    }
    private val isShare: Boolean by lazy {
        arguments!!.getBoolean(ARGS_SHARE)
    }
    private val fromConversation: Boolean by lazy {
        arguments!!.getBoolean(ARGS_FROM_CONVERSATION)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_forward, container, false)

    private fun setForwardText() {
        if (adapter.selectItem.size > 0) {
            forward_group.visibility = View.VISIBLE
        } else {
            forward_group.visibility = View.GONE
        }
        val str = StringBuffer()
        for (i in adapter.selectItem.size - 1 downTo 0) {
            adapter.selectItem[i].let {
                if (it is ConversationItem) {
                    if (it.isGroup()) {
                        str.append(it.groupName)
                    } else {
                        str.append(it.name)
                    }
                    if (i != 0) {
                        str.append(getString(R.string.divide))
                    }
                } else if (it is User) {
                    str.append(it.fullName)
                    if (i != 0) {
                        str.append(getString(R.string.divide))
                    }
                } else {
                }
            }
        }
        forward_tv.text = str
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (isShare) {
            title_view.title_tv.text = getString(R.string.share)
        }
        title_view.setOnClickListener {
            search_et?.hideKeyboard()
            activity?.onBackPressed()
        }
        forward_rv.adapter = adapter
        forward_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        forward_bn.setOnClickListener {
            search_et?.hideKeyboard()
            sendMessages(adapter.selectItem.size == 1)
        }
        adapter.setForwardListener(object : ForwardAdapter.ForwardListener {
            override fun onConversationItemClick(item: ConversationItem) {
                if (adapter.selectItem.contains(item)) {
                    adapter.selectItem.remove(item)
                } else {
                    adapter.selectItem.add(item)
                }
                setForwardText()
            }

            override fun onUserItemClick(user: User) {
                if (adapter.selectItem.contains(user)) {
                    adapter.selectItem.remove(user)
                } else {
                    adapter.selectItem.add(user)
                }
                setForwardText()
            }
        })
        search_et.addTextChangedListener(mWatcher)

        loadData()
    }

    private fun loadData() = lifecycleScope.launch {
        val conversations = chatViewModel.successConversationList()
        adapter.sourceConversations = conversations
        val set = ArraySet<String>()
        conversations.forEach { item ->
            if (item.isContact()) {
                set.add(item.ownerId)
            }
        }
        val list = chatViewModel.getFriends()
        if (list.isNotEmpty()) {
            adapter.sourceFriends = list.filter { item ->
                !set.contains(item.userId)
            }
        } else {
            adapter.sourceFriends = list
        }
        adapter.changeData()
    }

    private fun sendMessages(single: Boolean) {
        if (messages?.find { it.type == ForwardCategory.VIDEO.name || it.type == ForwardCategory.IMAGE.name || it.type == ForwardCategory.DATA.name } != null) {
            RxPermissions(requireActivity())
                .request(
                    WRITE_EXTERNAL_STORAGE,
                    READ_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe({ granted ->
                    if (granted) {
                        sharePreOperation(single)
                    } else {
                        requireContext().openPermissionSetting()
                    }
                }, {
                    Bugsnag.notify(it)
                })
        } else {
            sharePreOperation(single)
        }
    }

    private fun sharePreOperation(single: Boolean) {
        chatViewModel.sendForwardMessages(adapter.selectItem, messages, !isShare && !fromConversation)
        val forwardEvent = adapter.selectItem[0].let {
             if (it is User) {
                ForwardEvent(null, it.userId)
            } else {
                it as ConversationItem
                ForwardEvent(it.conversationId, null)
            }
        }
        if (isShare) {
            MainActivity.reopen(requireContext())
            activity?.finish()
            ConversationActivity.show(requireContext(), forwardEvent.conversationId, forwardEvent.userId)
        } else {
            activity?.finish()
            if (fromConversation && single) {
                RxBus.publish(forwardEvent)
            }
        }
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
