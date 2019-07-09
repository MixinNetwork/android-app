package one.mixin.android.ui.wallet

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import kotlinx.android.synthetic.main.fragment_single_friend_select.*
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.wallet.adapter.SingleFriendSelectAdapter
import one.mixin.android.vo.User
import one.mixin.android.widget.SearchView
import javax.inject.Inject

class SingleFriendSelectFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val chatViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val adapter by lazy {
        SingleFriendSelectAdapter()
    }
    var conversations: List<User>? = null
    var friends: List<User>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_single_friend_select, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.setOnClickListener {
            search_et.hideKeyboard()
            view?.findNavController()?.navigateUp()
        }
        transactions_rv.layoutManager = LinearLayoutManager(requireContext())
        transactions_rv.adapter = adapter
        transactions_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        adapter.listener = object : SingleFriendSelectAdapter.FriendSelectListener {
            override fun onItemClick(user: User) {
                TransferFragment.newInstance(user.userId)
                    .showNow(requireFragmentManager(), TransferFragment.TAG)
                view?.findNavController()?.navigateUp()
            }
        }
        chatViewModel.findContactUsers().observe(this, Observer { data ->
            data?.let { list ->
                conversations = list
                adapter.conversations = list
                chatViewModel.findFriendsNotBot().observe(this, Observer { r ->
                    if (r != null) {
                        val mutableList = mutableListOf<User>()
                        mutableList.addAll(r)
                        if (adapter.conversations != null) {
                            for (c in adapter.conversations!!) {
                                r.filter { c.userId == it.userId }
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
        search_et.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
                adapter.conversations = conversations?.filter {
                    it.fullName != null && it.fullName.contains(s.toString(), ignoreCase = true)
                }
                adapter.friends = friends?.filter {
                    it.fullName != null && it.fullName.contains(s.toString(), ignoreCase = true)
                }
                adapter.showHeader = s.isNullOrEmpty()
                adapter.notifyDataSetChanged()
            }

            override fun onSearch() {
            }
        }
    }
}