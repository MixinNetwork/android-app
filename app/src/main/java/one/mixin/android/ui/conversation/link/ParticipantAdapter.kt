package one.mixin.android.ui.conversation.link

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_link.view.*
import one.mixin.android.R
import one.mixin.android.extension.notNullElse
import one.mixin.android.vo.User

class ParticipantAdapter(val list: ArrayList<User>) : RecyclerView.Adapter<ParticipantAdapter.ParticipantHolder>() {
    override fun onBindViewHolder(holder: ParticipantHolder, position: Int) {
        list[position].let {
            holder.bind(it)
        }
    }

    override fun getItemCount(): Int =
        notNullElse(list, { it.size }, 0)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantHolder =
        ParticipantHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_link, parent, false))

    class ParticipantHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
        fun bind(user: User) {
            itemView.avatar_av.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
            itemView.name.text = user.fullName
        }
    }
}