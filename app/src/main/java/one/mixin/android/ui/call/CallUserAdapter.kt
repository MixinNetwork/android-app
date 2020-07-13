package one.mixin.android.ui.call

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_call_user.view.*
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.inflate
import one.mixin.android.vo.User

class CallUserAdapter(private val self: User) : ListAdapter<User, CallUserHolder>(User.DIFF_CALLBACK) {
    var guestsNotConnected: List<String>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CallUserHolder(parent.inflate(R.layout.item_call_user))

    override fun onBindViewHolder(holder: CallUserHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, self, guestsNotConnected, itemCount)
        }
    }

    override fun onCurrentListChanged(previousList: MutableList<User>, currentList: MutableList<User>) {
        val p = previousList.size
        val c = currentList.size
        if ((p <= 2 && c > 2) ||
            (p > 2 && c <= 2) ||
            (p in 3..10 && c !in 3..10) ||
            (p !in 3..10 && c in 3..10)
        ) {
            notifyDataSetChanged()
        }
    }
}

class CallUserHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val max = 96.dp
    private val mid = 76.dp
    private val min = 64.dp
    private val offset = 20.dp

    fun bind(user: User, self: User, guestsNotConnected: List<String>?, renderSize: Int) {
        itemView.apply {
            val size = getSize(renderSize)
            updateLayoutParams<ViewGroup.LayoutParams> {
                height = size + offset
            }
            avatar_view.updateLayoutParams<ViewGroup.LayoutParams> {
                width = size
                height = size
            }
            connecting_view.updateLayoutParams<ViewGroup.LayoutParams> {
                width = size
                height = size
            }
            avatar_view.setInfo(user.fullName, user.avatarUrl, user.userId)
            connecting_view.isVisible = user.userId != self.userId &&
                guestsNotConnected?.contains(user.userId) == true
        }
    }

    private fun getSize(itemCount: Int) = when {
        itemCount <= 2 -> max
        itemCount <= 9 -> mid
        else -> min
    }
}
