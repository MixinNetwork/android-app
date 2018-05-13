package one.mixin.android.ui.forward

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import kotlinx.android.synthetic.main.fragment_forward.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_MESSAGES
import one.mixin.android.ui.forward.ForwardActivity.Companion.ARGS_SHARE
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.User
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.support.v4.ctx
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

    private val adapter = ForwardAdapter()
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (isShare) {
            title_view.title_tv.text = getString(R.string.share_file)
        }
        title_view.setOnClickListener { activity?.onBackPressed() }
        forward_rv.adapter = adapter
        forward_rv.addItemDecoration(SpaceItemDecoration())
        forward_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        adapter.setForwardListener(object : ForwardAdapter.ForwardListener {
            override fun onConversationItemClick(item: ConversationItem) {
                sharePreOperation()
                ConversationActivity.show(context!!, item.conversationId,
                    null, messages = messages, isGroup = item.isGroup(), isBot = item.isBot())
            }

            override fun onUserItemClick(user: User) {
                sharePreOperation()
                ConversationActivity.show(ctx, null, user, messages = messages)
            }
        })

        chatViewModel.getConversations().observe(this, android.arch.lifecycle.Observer {
            it?.let {
                conversations = it
                adapter.conversations = it.filter { conversationItem ->
                    conversationItem.status == ConversationStatus.SUCCESS.ordinal
                }

                chatViewModel.getFriends().observe(this, android.arch.lifecycle.Observer { r ->
                    if (r != null) {
                        val mutableList = mutableListOf<User>()
                        mutableList.addAll(r)
                        if (adapter.conversations != null) {
                            for (c in adapter.conversations!!) {
                                r.filter { c.isContact() && c.ownerIdentityNumber == it.identityNumber }
                                    .forEach { mutableList.remove(it) }
                            }
                        }
                        friends = mutableList
                        adapter.friends = mutableList
                    }
                    adapter.notifyDataSetChanged()
                })
            }
        })
        search_et.addTextChangedListener(mWatcher)
    }

    private fun sharePreOperation() {
        if (isShare) {
            startActivity(Intent(context, MainActivity::class.java))
            activity?.finish()
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            adapter.conversations = conversations?.filter {
                if (it.isGroup()) {
                    it.groupName != null && (it.groupName.contains(s.toString()))
                } else {
                    it.name.contains(s.toString())
                }
            }
            adapter.friends = friends?.filter {
                it.fullName != null && it.fullName.contains(s.toString())
            }
            adapter.showHeader = s.isNullOrEmpty()
            adapter.notifyDataSetChanged()
        }
    }
}
