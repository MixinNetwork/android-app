package one.mixin.android.ui.panel.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_panel_contact.view.*
import one.mixin.android.R
import one.mixin.android.vo.User

class PanelContactAdapter : ListAdapter<User, PanelContactAdapter.PanelContactHolder>(User.DIFF_CALLBACK) {
    internal val selectedSet = ArraySet<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanelContactHolder =
        PanelContactHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_panel_contact, parent, false))

    override fun onBindViewHolder(holder: PanelContactHolder, position: Int) {
        val user = getItem(position)
        val view = holder.itemView
        view.avatar.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
        view.name.text = user.fullName
        if (selectedSet.contains(user.userId)) {
            view.avatar.setBorder(view.context.getColor(R.color.wallet_blue_secondary))
        } else {
            view.avatar.setBorder()
        }
        view.setOnClickListener {
            if (selectedSet.contains(user.userId)) {
                selectedSet.remove(user.userId)
            } else {
                selectedSet.add(user.userId)
            }
            onContactListener?.onContactSizeChanged(selectedSet.size)
            notifyItemChanged(position)
        }
    }

    class PanelContactHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    var onContactListener: OnContactListener? = null

    interface OnContactListener {
        fun onContactSizeChanged(size: Int)
    }
}