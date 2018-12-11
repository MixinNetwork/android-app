package one.mixin.android.ui.panel.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_panel_contact.view.*
import one.mixin.android.vo.User
import one.mixin.android.R

class PanelContactAdapter : ListAdapter<User, PanelContactAdapter.PanelContactHolder>(User.DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanelContactHolder =
        PanelContactHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_panel_contact, parent, false))

    override fun onBindViewHolder(holder: PanelContactHolder, position: Int) {
        val user = getItem(position)
        val view = holder.itemView
        view.avatar.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
        view.name.text = user.fullName
    }

    class PanelContactHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}