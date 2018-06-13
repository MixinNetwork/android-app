package one.mixin.android.ui.conversation

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import kotlinx.android.synthetic.main.fragment_friends.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.conversation.ConversationFragment.Companion.CONVERSATION_ID
import one.mixin.android.ui.conversation.adapter.FriendAdapter
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.User
import javax.inject.Inject

class FriendsFragment : BaseFragment(), FriendAdapter.FriendListener {

    companion object {
        const val TAG = "FriendsFragment"
        const val ARGS_IS_GROUP = "args_is_group"
        const val ARGS_IS_BOT = "args_is_bot"

        fun newInstance(conversationId: String, isGroup: Boolean, isBot: Boolean) = FriendsFragment().apply {
            arguments = bundleOf(
                CONVERSATION_ID to conversationId,
                ARGS_IS_GROUP to isGroup,
                ARGS_IS_BOT to isBot
            )
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val conversationViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }
    @Inject
    lateinit var jobManager: MixinJobManager

    private val adapter = FriendAdapter().apply { listener = this@FriendsFragment }
    private val conversationId: String by lazy { arguments!!.getString(CONVERSATION_ID) }
    private val isGroup: Boolean by lazy { arguments!!.getBoolean(ARGS_IS_GROUP) }
    private val isBot: Boolean by lazy { arguments!!.getBoolean(ARGS_IS_BOT) }

    private var users = arrayListOf<User>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        layoutInflater.inflate(R.layout.fragment_friends, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener {
            search_et.hideKeyboard()
            activity?.onBackPressed()
        }
        friends_rv.adapter = adapter
        friends_rv.addItemDecoration(SpaceItemDecoration())
        conversationViewModel.getFriends().observe(this, Observer {
            if (it == null || it.isEmpty()) return@Observer

            users.clear()
            users.addAll(it)
            adapter.friends = it
        })

        search_et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                val us = arrayListOf<User>()
                users.forEach {
                    if (it.fullName?.contains(s, true) == true) {
                        us.add(it)
                    }
                }
                adapter.friends = us
            }
        })
    }

    override fun onFriendClick(user: User) {
        val fw = ForwardMessage(ForwardCategory.CONTACT.name, sharedUserId = user.userId)
        ConversationActivity.show(requireContext(), conversationId, null, messages = arrayListOf(fw))
    }
}