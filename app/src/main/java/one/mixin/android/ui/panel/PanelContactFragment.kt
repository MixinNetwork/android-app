package one.mixin.android.ui.panel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_panel_contact.*
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.panel.adapter.PanelContactAdapter
import one.mixin.android.ui.panel.listener.OnSendContactsListener
import one.mixin.android.vo.ForwardMessage
import javax.inject.Inject

class PanelContactFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val conversationViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val adapter = PanelContactAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_panel_contact, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contact_rv.layoutManager = GridLayoutManager(context, 3)
        contact_rv.adapter = adapter
        adapter.onContactListener = object : PanelContactAdapter.OnContactListener {
            override fun onSendContact( msg:ForwardMessage) {
                onSendContactsListener?.onSendContacts(msg)
            }
        }

        conversationViewModel.getFriends().observe(this, Observer {
            adapter.submitList(it)
        })
    }

    fun getSelectAndClear(): String? {
        val uid = adapter.selectedUserId
        adapter.selectedUserId = null
        adapter.notifyDataSetChanged()
        return uid
    }

    var onSendContactsListener: OnSendContactsListener? = null

    companion object {
        const val TAG = "PanelContactFragment"

        fun newInstance() = PanelContactFragment()
    }
}