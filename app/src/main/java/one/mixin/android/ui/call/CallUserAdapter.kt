package one.mixin.android.ui.call

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemCallAddBinding
import one.mixin.android.databinding.ItemCallUserBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.round
import one.mixin.android.vo.CallUser

class CallUserAdapter(private val self: CallUser, private val onClickListener: View.OnClickListener) :
    ListAdapter<CallUser, RecyclerView.ViewHolder>(CallUser.DIFF_CALLBACK) {
    var guestsNotConnected: List<String>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        if (viewType == 1) {
            CallUserHolder(
                ItemCallUserBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            AddUserHolder(
                ItemCallAddBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            getItem(position - 1)?.let {
                (holder as CallUserHolder).bind(it, self, guestsNotConnected)
            }
        } else {
            (holder as AddUserHolder).bind(onClickListener)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            0
        } else {
            1
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + 1
    }

    override fun onCurrentListChanged(previousList: MutableList<CallUser>, currentList: MutableList<CallUser>) {
        if (previousList != currentList) {
            notifyDataSetChanged()
        }
    }
}

class AddUserHolder(val binding: ItemCallAddBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(onClickListener: View.OnClickListener) {
        itemView.setOnClickListener(onClickListener)
    }
}

class CallUserHolder(val binding: ItemCallUserBinding) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.loading.round(64.dp)
    }

    fun bind(
        user: CallUser,
        self: CallUser,
        guestsNotConnected: List<String>?,
    ) {
        itemView.apply {
            binding.avatarView.setInfo(user.fullName, user.avatarUrl, user.userId)
            binding.nameTv.text = user.fullName
            binding.loading.setAutoRepeat(true)
            binding.loading.setAnimation(R.raw.anim_call_loading, 64.dp, 64.dp)
            binding.loading.playAnimation()
            val vis =
                user.userId != self.userId && guestsNotConnected?.contains(user.userId) == true
            binding.loading.isVisible = vis
            binding.cover.isVisible = vis
        }
    }
}
