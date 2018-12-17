package one.mixin.android.ui.panel

import android.os.Bundle
import android.text.Editable
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.bottom_sheet_panel_contact.view.*
import one.mixin.android.R
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.panel.adapter.PanelContactAdapter
import one.mixin.android.ui.panel.listener.OnSendContactsListener
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.User
import one.mixin.android.widget.SearchView

class PanelContactBottomSheet : PanelBottomSheet() {

    private val conversationViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val adapter = PanelContactAdapter()

    private var friends: List<User>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.contact_rv.layoutManager = GridLayoutManager(context, 3)
        contentView.contact_rv.adapter = adapter
        adapter.onContactListener = object : PanelContactAdapter.OnContactListener {
            override fun onContactSizeChanged(size: Int) {
                contentView.send_tv.visibility = if (size == 0) GONE else VISIBLE
                contentView.send_tv.text = getString(R.string.panel_contact_send, size)
            }
        }
        contentView.send_tv.setOnClickListener {
            val msgArray = arrayListOf<ForwardMessage>()
            adapter.selectedSet.forEach { userId ->
                msgArray.add(ForwardMessage(ForwardCategory.CONTACT.name, sharedUserId = userId))
            }
            onSendContactsListener?.onSendContacts(msgArray)
            dismiss()
        }
        contentView.search_et.listener = object : SearchView.OnSearchViewListener {
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

    override fun getContentViewId() = R.layout.bottom_sheet_panel_contact

    var onSendContactsListener: OnSendContactsListener? = null

    companion object {
        const val TAG = "PanelContactBottomSheet"

        fun newInstance() = PanelContactBottomSheet()
    }
}