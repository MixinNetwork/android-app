package one.mixin.android.ui.call

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_call_user.view.*
import one.mixin.android.R
import one.mixin.android.extension.inflate
import one.mixin.android.vo.User

class CallUserAdapter(private val self: User) : ListAdapter<User, CallUserHolder>(User.DIFF_CALLBACK) {
    var guestsNotConnected: ArrayList<String>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CallUserHolder(parent.inflate(R.layout.item_call_user))

    override fun onBindViewHolder(holder: CallUserHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, self, guestsNotConnected)
        }
    }
}

class CallUserHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(user: User, self: User, guestsNotConnected: ArrayList<String>?) {
        itemView.avatar_view.setInfo(user.fullName, user.avatarUrl, user.userId)
        itemView.connecting_view.isVisible = user.userId != self.userId &&
            guestsNotConnected?.contains(user.userId) == true
    }
}
