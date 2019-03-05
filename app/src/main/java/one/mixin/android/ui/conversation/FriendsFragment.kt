package one.mixin.android.ui.conversation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import kotlinx.android.synthetic.main.fragment_friends.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.FriendAdapter
import one.mixin.android.vo.User
import org.jetbrains.anko.textColor
import java.util.Arrays
import javax.inject.Inject

class FriendsFragment : BaseFragment(), FriendAdapter.FriendListener {

    companion object {
        const val TAG = "FriendsFragment"

        fun newInstance() = FriendsFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val conversationViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }
    @Inject
    lateinit var jobManager: MixinJobManager

    private val adapter = FriendAdapter().apply { listener = this@FriendsFragment }

    private var users = arrayListOf<User>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        layoutInflater.inflate(R.layout.fragment_friends, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.right_animator.setOnClickListener {
            friendClick?.invoke(adapter.selectedFriends.keys.toSet())
            requireFragmentManager().beginTransaction().remove(this).commit()
        }
        title_view.left_ib.setOnClickListener {
            search_et.hideKeyboard()
            activity?.onBackPressed()
        }
        friends_rv.adapter = adapter
        friends_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        conversationViewModel.getFriends().observe(this, Observer {
            if (it == null || it.isEmpty()) return@Observer

            val array = it.toTypedArray()
            Arrays.sort<User>(array) { u1, u2 ->
                if (u1.fullName == null) return@sort -1
                if (u2.fullName == null) return@sort 1
                return@sort u1.fullName[0] - u2.fullName[0]
            }
            val list = array.toList()
            users.clear()
            users.addAll(list)
            adapter.friends = list
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

    private var friendClick: ((Set<String>) -> Unit)? = null

    fun setOnFriendClick(friendClick: (Set<String>) -> Unit) {
        this.friendClick = friendClick
    }

    override fun onFriendClick(pos: Int) {
        if (adapter.selectedFriends.isEmpty()) {
            title_view.right_tv.textColor = resources.getColor(R.color.text_gray, null)
            title_view.right_animator.isEnabled = false
        } else {
            title_view.right_tv.textColor = resources.getColor(R.color.wallet_blue_secondary, null)
            title_view.right_animator.isEnabled = true
        }
    }
}