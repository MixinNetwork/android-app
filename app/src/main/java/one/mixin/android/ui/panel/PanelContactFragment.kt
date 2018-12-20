package one.mixin.android.ui.panel

import android.os.Bundle
import android.text.Editable
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_panel_contact.*
import one.mixin.android.R
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.panel.adapter.PanelContactAdapter
import one.mixin.android.ui.panel.listener.OnSendContactsListener
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.User
import one.mixin.android.widget.SearchView
import javax.inject.Inject

class PanelContactFragment : PanelBarFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val conversationViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val adapter = PanelContactAdapter()

    private var friends: List<User>? = null

    override fun getLayoutId() = R.layout.fragment_panel_contact

    override fun beforeExpand() {
        search_et.visibility = VISIBLE
    }

    override fun afterCollapse() {
        search_et.visibility = GONE
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contact_rv.layoutManager = GridLayoutManager(context, 3)
        contact_rv.adapter = adapter
        adapter.onContactListener = object : PanelContactAdapter.OnContactListener {
            override fun onSendContact(msg: ForwardMessage) {
                onSendContactsListener?.onSendContacts(msg)
            }
        }

        search_et.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
                adapter.submitList(if (s.isNullOrBlank()) {
                    friends
                } else {
                    friends?.filter { it.fullName == null || it.fullName.startsWith(s, true) }
                })
            }

            override fun onSearch() {
                // Left empty for local data filter
            }
        }

        conversationViewModel.getFriends().observe(this, Observer {
            friends = it
            adapter.submitList(it)
        })
    }

    var onSendContactsListener: OnSendContactsListener? = null

    companion object {
        const val TAG = "PanelContactFragment"

        fun newInstance() = PanelContactFragment()
    }
}