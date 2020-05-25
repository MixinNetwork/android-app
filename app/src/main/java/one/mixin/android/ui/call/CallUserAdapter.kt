package one.mixin.android.ui.call

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.extension.inflate
import one.mixin.android.vo.User

class CallUserAdapter : ListAdapter<User, CallUserHolder>(User.DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CallUserHolder(parent.inflate(R.layout.item_call_user))

    override fun onBindViewHolder(holder: CallUserHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }
}

class CallUserHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(user: User) {

    }
}