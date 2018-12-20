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

    private var selectedIndex = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanelContactHolder =
        PanelContactHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_panel_contact, parent, false))

    override fun onBindViewHolder(holder: PanelContactHolder, position: Int) {
        val user = getItem(position)
        val view = holder.itemView
        view.avatar.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
        view.name.text = user.fullName
        if (selectedIndex == position) {
            view.blur.showBlur(view.avatar, user.userId)
        } else {
            view.blur.hideBlur()
        }
        view.setOnClickListener {
            notifyItemChanged(selectedIndex)
            if (selectedIndex == position) {
                selectedIndex = -1
                onContactListener?.onSendContact(ForwardMessage(ForwardCategory.CONTACT.name, sharedUserId = user.userId))
                notifyItemChanged(position)
            } else {
                selectedIndex = position
                notifyItemChanged(position)
            }
        }
    }

    class PanelContactHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    var onContactListener: OnContactListener? = null

    interface OnContactListener {
        fun onSendContact(msg: ForwardMessage)
    }
}