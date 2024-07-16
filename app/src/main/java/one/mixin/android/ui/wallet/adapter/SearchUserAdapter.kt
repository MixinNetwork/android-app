package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemContactNormalBinding
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class SearchUserAdapter() : ListAdapter<User, UserHolder>(User.DIFF_CALLBACK) {
    var callback: WalletSearchUserCallback? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): UserHolder {
        return UserHolder(ItemContactNormalBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(
        holder: UserHolder,
        position: Int,
    ) {
        getItem(position)?.let { holder.bind(it, callback) }
    }
}

class UserHolder(val binding: ItemContactNormalBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        user: User,
        callback: WalletSearchUserCallback? = null,
    ) {
        binding.apply {
            normal.text = user.fullName
            // normal.highLight(filter)
            mixinIdTv.text = user.identityNumber
            // mixinIdTv.highLight(filter)
            avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            user.showVerifiedOrBot(verifiedIv, botIv)
            root.setOnClickListener {
                callback?.onUserClick(user)
            }
        }
    }
}

interface WalletSearchUserCallback {
    fun onUserClick(
        user: User
    )
}