package one.mixin.android.ui.contacts

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.android.synthetic.main.item_contact_contact.view.*
import kotlinx.android.synthetic.main.item_contact_header.view.*
import kotlinx.android.synthetic.main.item_contact_normal.view.*
import kotlinx.android.synthetic.main.view_contact_header.view.*
import kotlinx.android.synthetic.main.view_contact_list_empty.view.*
import one.mixin.android.R
import one.mixin.android.extension.inflate
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
    private var mFooterView: View? = null
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
        return if (u.fullName != null && u.fullName.isNotEmpty()) u.fullName[0].toLong() else -1L
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
        val view = parent.inflate(R.layout.item_contact_header, false)
        return HeaderViewHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        if (friendSize > 0 && position < POS_FRIEND + friendSize && position >= POS_HEADER) {
            holder.bind()
            return
        }
        val user = users[getPosition(position)]
        holder.bind(user)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (mHeaderView != null && viewType == TYPE_HEADER) {
            HeadViewHolder(mHeaderView!!)
        } else if (mFooterView != null && viewType == TYPE_FOOTER) {
            FootViewHolder(mFooterView!!)
        } else if (viewType == TYPE_CONTACT) {
            val view = parent.inflate(R.layout.item_contact_contact, false)
            ContactViewHolder(view)
        } else {
            val view = parent.inflate(R.layout.item_contact_friend, false)
            FriendViewHolder(view)
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

    fun setHeader(view: View) {
        mHeaderView = view
    }

    fun setFooter(view: View) {
        mFooterView = view
    }

    fun showEmptyFooter() {
        mFooterView?.apply {
            empty_rl.isVisible = true
            empty_tip_tv.isVisible = true
        }
    }

    fun hideEmptyFooter() {
        mFooterView?.apply {
            empty_rl.isVisible = false
            empty_tip_tv.isVisible = false
        }
    }

    fun removeFooter() {
        mFooterView = null
    }

    fun setContactListener(listener: ContactListener) {
        mContactListener = listener
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            itemView.header.text = itemView.context.getString(R.string.contact_item_title)
        }

        fun bind(user: User) {
            itemView.header.text = if (user.fullName != null &&
                user.fullName.isNotEmpty()
            ) user.fullName[0].toString() else ""
        }
    }

    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class HeadViewHolder(itemView: View) : ViewHolder(itemView) {
        fun bind(self: User?, listener: ContactListener?) {
            val account = Session.getAccount()
            if (self != null) {
                itemView.contact_header_avatar.setInfo(self.fullName, self.avatarUrl, self.userId)
                itemView.contact_header_name_tv.text = self.fullName
                itemView.contact_header_id_tv.text =
                    itemView.context.getString(R.string.contact_mixin_id, self.identityNumber)
                itemView.contact_header_mobile_tv.text =
                    itemView.context.getString(R.string.contact_mobile, self.phone)
            } else {
                if (account != null) {
                    itemView.contact_header_avatar.setInfo(account.fullName, account.avatarUrl, account.userId)
                    itemView.contact_header_name_tv.text = account.fullName
                    itemView.contact_header_id_tv.text =
                        itemView.context.getString(R.string.contact_mixin_id, account.identityNumber)
                    itemView.contact_header_mobile_tv.text =
                        itemView.context.getString(R.string.contact_mobile, account.phone)
                }
            }
            if (listener != null) {
                itemView.contact_header_rl.setOnClickListener { listener.onHeaderRl() }
                itemView.new_group_rl.setOnClickListener { listener.onNewGroup() }
                itemView.add_contact_rl.setOnClickListener { listener.onAddContact() }
                itemView.my_qr_fl.setOnClickListener { listener.onMyQr(self) }
                itemView.receive_fl.setOnClickListener { listener.onReceiveQr(self) }
            }
        }
    }

    class FootViewHolder(itemView: View) : ViewHolder(itemView) {
        fun bind(listener: ContactListener?, friendSize: Int) {
            if (listener != null) {
                itemView.empty_rl.setOnClickListener { listener.onEmptyRl() }
                itemView.count_tv.text = itemView.context.getString(R.string.contact_count, friendSize)
            }
        }
    }

    class FriendViewHolder(itemView: View) : ViewHolder(itemView) {
        fun bind(user: User, listener: ContactListener?) {
            itemView.normal.text = user.fullName
            itemView.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            user.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)
            if (listener != null) {
                itemView.setOnClickListener { listener.onFriendItem(user) }
            }
        }
    }

    class ContactViewHolder(itemView: View) : ViewHolder(itemView) {
        fun bind(user: User, listener: ContactListener?) {
            itemView.index.text = if (user.fullName != null && user.fullName.isNotEmpty())
                user.fullName[0].toString() else ""
            itemView.contact_friend.text = user.fullName
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
