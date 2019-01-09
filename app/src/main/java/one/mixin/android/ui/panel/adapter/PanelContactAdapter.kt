package one.mixin.android.ui.panel.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_panel_contact.view.*
import one.mixin.android.R
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.User

class PanelContactAdapter : ListAdapter<User, PanelContactAdapter.PanelContactHolder>(User.DIFF_CALLBACK) {

    var selectedUserId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanelContactHolder =
        PanelContactHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_panel_contact, parent, false))

    override fun onBindViewHolder(holder: PanelContactHolder, position: Int) {
        val user = getItem(position)
        val view = holder.itemView
        view.avatar.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
        view.name.text = user.fullName
        if (selectedUserId == user.userId) {
            view.blur.showBlur(view.avatar)
        } else {
            view.blur.hideBlur()
        }
        view.setOnClickListener {
            if (selectedUserId == user.userId) {
                selectedUserId = null
                onContactListener?.onSendContact(ForwardMessage(ForwardCategory.CONTACT.name, sharedUserId = user.userId))
                notifyItemChanged(position)
            } else {
                selectedUserId = user.userId
                notifyDataSetChanged()
            }
        }
    }

    class PanelContactHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    var onContactListener: OnContactListener? = null

    interface OnContactListener {
        fun onSendContact(msg: ForwardMessage)
    }
}