package one.mixin.android.ui.forward

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import com.bugsnag.android.Bugsnag
import com.tbruyelle.rxpermissions2.RxPermissions
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import kotlinx.android.synthetic.main.fragment_forward.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_MESSAGES
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_SHARE
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.User
import javax.inject.Inject

class ForwardFragment : BaseFragment() {
    companion object {
        const val TAG = "ForwardFragment"

        fun newInstance(messages: ArrayList<ForwardMessage>, isShare: Boolean = false): ForwardFragment {
            val fragment = ForwardFragment()
            val b = bundleOf(
                ARGS_MESSAGES to messages,
                ARGS_SHARE to isShare
            )
            fragment.arguments = b
            return fragment
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val chatViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val adapter by lazy {
        ForwardAdapter()
    }
    var conversations: List<ConversationItem>? = null
    var friends: List<User>? = null

    private val messages: ArrayList<ForwardMessage>? by lazy {
        arguments!!.getParcelableArrayList<ForwardMessage>(ARGS_MESSAGES)
    }

    private val isShare: Boolean by lazy {
        arguments!!.getBoolean(ARGS_SHARE)
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
                        str.append("、")
                    }
                } else if (it is User) {
                    str.append(it.fullName)
                    if (i != 0) {
                        str.append("、")
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
        title_view.setOnClickListener { activity?.onBackPressed() }
        forward_rv.adapter = adapter
        forward_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        forward_bn.setOnClickListener {
            if (adapter.selectItem.size == 1) {
                adapter.selectItem[0].let {
                    if (it is User) {
                        sendSingleMessage(null, it.userId)
                    } else if (it is ConversationItem) {
                        sendSingleMessage(it.conversationId, null)
                    }
                }
            } else {
                sendMessages()
            }
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

        chatViewModel.successConversationList().observe(this, Observer {
            it?.let { conversations ->
                val set = ArraySet<String>()
                this.conversations = conversations
                adapter.sourceConversations = conversations
                conversations.forEach { item ->
                    if (item.isContact()) {
                        set.add(item.ownerId)
                    }
                }

                chatViewModel.viewModelScope.launch {
                    val list = chatViewModel.getFriends()
                    if (list.isNotEmpty()) {
                        friends = list.filter { item ->
                            !set.contains(item.userId)
                        }
                        adapter.sourceFriends = friends
                    } else {
                        friends = list
                        adapter.sourceFriends = list
                    }
                    adapter.changeData()
                }
            }
        })
        search_et.addTextChangedListener(mWatcher)
    }

    @SuppressLint("CheckResult", "AutoDispose")
    private fun sendMessages() {
        if (messages?.find { it.type == ForwardCategory.VIDEO.name || it.type == ForwardCategory.IMAGE.name || it.type == ForwardCategory.DATA.name } != null) {
            RxPermissions(requireActivity())
                .request(
                    WRITE_EXTERNAL_STORAGE,
                    READ_EXTERNAL_STORAGE)
                .subscribe({ granted ->
                    if (granted) {
                        chatViewModel.sendForwardMessages(adapter.selectItem, messages)
                        requireActivity().finish()
                        sharePreOperation()
                    } else {
                        requireContext().openPermissionSetting()
                    }
                }, {
                    Bugsnag.notify(it)
                })
        } else {
            chatViewModel.sendForwardMessages(adapter.selectItem, messages)
            sharePreOperation()
        }
    }

    @SuppressLint("CheckResult", "AutoDispose")
    private fun sendSingleMessage(conversationId: String?, userId: String?) {
        if (messages?.find { it.type == ForwardCategory.VIDEO.name || it.type == ForwardCategory.IMAGE.name || it.type == ForwardCategory.DATA.name } != null) {
            RxPermissions(requireActivity())
                .request(
                    WRITE_EXTERNAL_STORAGE,
                    READ_EXTERNAL_STORAGE)
                .subscribe({ granted ->
                    if (granted) {
                        sharePreOperation()
                        ConversationActivity.show(requireContext(), conversationId, userId, messages = messages)
                    } else {
                        requireContext().openPermissionSetting()
                    }
                }, {
                    Bugsnag.notify(it)
                })
        } else {
            sharePreOperation()
            ConversationActivity.show(requireContext(), conversationId, userId, messages = messages)
        }
    }

    private fun sharePreOperation() {
        if (isShare) {
            startActivity(Intent(context, MainActivity::class.java))
            activity?.finish()
        } else {
            activity?.finish()
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            adapter.name = s
            adapter.changeData()
        }
    }
}
