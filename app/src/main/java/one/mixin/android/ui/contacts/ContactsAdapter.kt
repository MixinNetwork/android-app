package one.mixin.android.ui.contacts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.databinding.ItemContactContactBinding
import one.mixin.android.databinding.ItemContactFriendBinding
import one.mixin.android.databinding.ItemContactHeaderBinding
import one.mixin.android.databinding.ViewContactHeaderBinding
import one.mixin.android.databinding.ViewContactListEmptyBinding
import one.mixin.android.session.Session
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class ContactsAdapter(val context: Context, var users: List<User>, var friendSize: Int) :
    RecyclerView.Adapter<ContactsAdapter.ViewHolder>(),
    StickyRecyclerHeadersAdapter<ContactsAdapter.HeaderViewHolder> {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_FOOTER = 1
        const val TYPE_FRIEND = 2
        const val TYPE_CONTACT = 3

        const val POS_HEADER = 0
        const val POS_FRIEND = 1
    }

    private var mHeaderView: View? = null
    private var mHeaderViewBinding: ViewContactHeaderBinding? = null
    private var mFooterView: View? = null
    private var mFooterViewBinding: ViewContactListEmptyBinding? = null
    private var mContactListener: ContactListener? = null
    var me: User? = null

    override fun getItemCount(): Int {
        return if (mHeaderView == null && mFooterView == null) {
            users.size
        } else if (mHeaderView == null && mFooterView != null) {
            users.size + 1
        } else if (mHeaderView != null && mFooterView == null) {
            users.size + 1
        } else {
            users.size + 2
        }
    }

    override fun getHeaderId(position: Int): Long {
        if (mHeaderView != null && position == POS_HEADER) {
            return -1
        } else if (mFooterView != null && position == itemCount - 1) {
            return -1
        } else if (friendSize > 0 && position >= POS_FRIEND && position < POS_FRIEND + friendSize) {
            return POS_FRIEND.toLong()
        }
        val u = users[getPosition(position)]
        return if (u.fullName != null && u.fullName.isNotEmpty()) u.fullName[0].code.toLong() else -1L
    }

    override fun getItemViewType(position: Int): Int {
        if (mHeaderView == null && mFooterView == null) {
            return if (friendSize == users.size) {
                TYPE_FRIEND
            } else {
                TYPE_CONTACT
            }
        }
        return when {
            position == POS_HEADER -> TYPE_HEADER
            position == itemCount - 1 -> {
                if (mFooterView != null) {
                    return TYPE_FOOTER
                }
                return if (friendSize == users.size) {
                    TYPE_FRIEND
                } else {
                    TYPE_CONTACT
                }
            }
            position < POS_FRIEND + friendSize -> TYPE_FRIEND
            else -> TYPE_CONTACT
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        return HeaderViewHolder(ItemContactHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        if (friendSize > 0 && position < POS_FRIEND + friendSize && position >= POS_HEADER) {
            holder.bind()
            return
        }
        val user = try {
            users[getPosition(position)]
        } catch (e: IndexOutOfBoundsException) {
            return
        }
        holder.bind(user)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (mHeaderView != null && viewType == TYPE_HEADER) {
            HeadViewHolder(mHeaderViewBinding!!)
        } else if (mFooterView != null && viewType == TYPE_FOOTER) {
            FootViewHolder(mFooterViewBinding!!)
        } else if (viewType == TYPE_CONTACT) {
            ContactViewHolder(ItemContactContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            FriendViewHolder(ItemContactFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is HeadViewHolder -> {
                holder.bind(me, mContactListener)
            }
            is FootViewHolder -> {
                holder.bind(mContactListener, friendSize)
            }
            is ContactViewHolder -> {
                val user: User = users[getPosition(position)]
                holder.bind(user, mContactListener)
            }
            else -> {
                holder as FriendViewHolder
                val user: User = users[getPosition(position)]
                holder.bind(user, mContactListener)
            }
        }
    }

    private fun getPosition(position: Int): Int {
        return if (mHeaderView != null) {
            position - 1
        } else {
            position
        }
    }

    fun setHeader(binding: ViewContactHeaderBinding) {
        mHeaderViewBinding = binding
        mHeaderView = binding.root
    }

    fun setFooter(binding: ViewContactListEmptyBinding) {
        mFooterViewBinding = binding
        mFooterView = binding.root
    }

    fun showEmptyFooter() {
        mFooterViewBinding?.apply {
            emptyRl.isVisible = true
            emptyTipTv.isVisible = true
        }
    }

    fun hideEmptyFooter() {
        mFooterViewBinding?.apply {
            emptyRl.isVisible = false
            emptyTipTv.isVisible = false
        }
    }

    fun removeFooter() {
        mFooterView = null
    }

    fun setContactListener(listener: ContactListener) {
        mContactListener = listener
    }

    class HeaderViewHolder(val binding: ItemContactHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.header.text = itemView.context.getString(R.string.CONTACTS)
        }

        fun bind(user: User) {
            binding.header.text = if (user.fullName != null &&
                user.fullName.isNotEmpty()
            ) user.fullName[0].toString() else ""
        }
    }

    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class HeadViewHolder(val binding: ViewContactHeaderBinding) : ViewHolder(binding.root) {
        fun bind(self: User?, listener: ContactListener?) {
            val account = Session.getAccount()
            if (self != null) {
                binding.contactHeaderAvatar.setInfo(self.fullName, self.avatarUrl, self.userId)
                binding.contactHeaderNameTv.text = self.fullName
                binding.contactHeaderIdTv.text =
                    itemView.context.getString(R.string.contact_mixin_id, self.identityNumber)
                binding.contactHeaderMobileTv.text =
                    itemView.context.getString(R.string.contact_mobile, self.phone)
            } else {
                if (account != null) {
                    binding.contactHeaderAvatar.setInfo(account.fullName, account.avatarUrl, account.userId)
                    binding.contactHeaderNameTv.text = account.fullName
                    binding.contactHeaderIdTv.text =
                        itemView.context.getString(R.string.contact_mixin_id, account.identityNumber)
                    binding.contactHeaderMobileTv.text =
                        itemView.context.getString(R.string.contact_mobile, account.phone)
                }
            }
            if (listener != null) {
                binding.contactHeaderRl.setOnClickListener { listener.onHeaderRl() }
                binding.newGroupRl.setOnClickListener { listener.onNewGroup() }
                binding.addContactRl.setOnClickListener { listener.onAddContact() }
                binding.myQrFl.setOnClickListener { listener.onMyQr(self) }
                binding.receiveFl.setOnClickListener { listener.onReceiveQr(self) }
            }
        }
    }

    class FootViewHolder(val binding: ViewContactListEmptyBinding) : ViewHolder(binding.root) {
        fun bind(listener: ContactListener?, friendSize: Int) {
            if (listener != null) {
                binding.emptyRl.setOnClickListener { listener.onEmptyRl() }
                binding.countTv.text = itemView.context.resources.getQuantityString(R.plurals.contact_count, friendSize, friendSize)
            }
        }
    }

    class FriendViewHolder(val binding: ItemContactFriendBinding) : ViewHolder(binding.root) {
        fun bind(user: User, listener: ContactListener?) {
            binding.root.normal.text = user.fullName
            binding.root.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            user.showVerifiedOrBot(binding.root.verifiedIv, binding.root.botIv)
            if (listener != null) {
                itemView.setOnClickListener { listener.onFriendItem(user) }
            }
        }
    }

    class ContactViewHolder(val binding: ItemContactContactBinding) : ViewHolder(binding.root) {
        fun bind(user: User, listener: ContactListener?) {
            binding.index.text = if (user.fullName != null && user.fullName.isNotEmpty())
                user.fullName[0].toString() else ""
            binding.contactFriend.text = user.fullName
            if (listener != null) {
                itemView.setOnClickListener { listener.onContactItem(user) }
            }
        }
    }

    interface ContactListener {
        fun onHeaderRl()
        fun onNewGroup()
        fun onAddContact()
        fun onEmptyRl()
        fun onFriendItem(user: User)
        fun onContactItem(user: User)
        fun onMyQr(self: User?)
        fun onReceiveQr(self: User?)
    }
}
