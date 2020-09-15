package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_blocked.*
import kotlinx.android.synthetic.main.item_contact_normal.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.vo.User

@AndroidEntryPoint
class SettingBlockedFragment : BaseViewModelFragment<SettingBlockedViewModel>() {
    companion object {
        const val TAG = "SettingBlockedFragment"
        const val POS_LIST = 0
        const val POS_EMPTY = 1

        fun newInstance(): SettingBlockedFragment {
            return SettingBlockedFragment()
        }
    }

    override fun getModelClass() = SettingBlockedViewModel::class.java

    private val adapter = BlockedAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_blocked, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        blocked_rv.adapter = adapter
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        viewModel.blockingUsers(stopScope).observe(
            viewLifecycleOwner,
            Observer {
                if (it != null && it.isNotEmpty()) {
                    block_va.displayedChild = POS_LIST
                    adapter.setUsers(it)
                } else {
                    block_va.displayedChild = POS_EMPTY
                }
            }
        )
    }

    class BlockedAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object {
            const val TYPE_NORMAL = 0
            const val TYPE_FOOTER = 1
        }

        private var users: List<User>? = null

        fun setUsers(users: List<User>) {
            this.users = users
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return if ((users == null || users!!.isEmpty()) || position == users!!.size) {
                TYPE_FOOTER
            } else {
                TYPE_NORMAL
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_FOOTER) {
                FooterHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_blocked_footer, parent, false))
            } else {
                ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_contact_normal, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (users == null || users!!.isEmpty()) {
                return
            }
            if (holder is ItemHolder) {
                holder.bind(users!![position])
            }
        }

        override fun getItemCount(): Int = (users?.size ?: 0) + 1
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: User) {
            itemView.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            itemView.normal.text = user.fullName
            itemView.setOnClickListener {
                UserBottomSheetDialogFragment.newInstance(user).show(
                    (it.context as FragmentActivity).supportFragmentManager,
                    UserBottomSheetDialogFragment.TAG
                )
            }
        }
    }

    class FooterHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
