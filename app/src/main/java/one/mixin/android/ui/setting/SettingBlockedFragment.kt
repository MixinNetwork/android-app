package one.mixin.android.ui.setting

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_blocked.*
import kotlinx.android.synthetic.main.item_contact_normal.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.notNullElse
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.vo.User
import javax.inject.Inject

class SettingBlockedFragment : BaseFragment() {
    companion object {
        const val TAG = "SettingBlockedFragment"
        const val POS_LIST = 0
        const val POS_EMPTY = 1

        fun newInstance(): SettingBlockedFragment {
            return SettingBlockedFragment()
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val settingBlockedViewModel: SettingBlockedViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(SettingBlockedViewModel::class.java)
    }
    private val adapter = BlockedAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_blocked, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        blocked_rv.adapter = adapter
        blocked_rv.addItemDecoration(SpaceItemDecoration(1))
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        settingBlockedViewModel.blockingUsers(scopeProvider).observe(this, Observer {
            if (it != null && it.isNotEmpty()) {
                block_va.displayedChild = POS_LIST
                adapter.setUsers(it)
            } else {
                block_va.displayedChild = POS_EMPTY
            }
        })
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

        override fun getItemCount(): Int = notNullElse(users, { it.size + 1 }, 1)
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: User) {
            itemView.avatar.setInfo(if (user.fullName != null && user.fullName.isNotEmpty()) user.fullName[0] else ' ',
                user.avatarUrl, user.identityNumber)
            itemView.normal.text = user.fullName
            itemView.setOnClickListener {
                UserBottomSheetDialogFragment.newInstance(user).show(
                    (it.context as FragmentActivity).supportFragmentManager, UserBottomSheetDialogFragment.TAG)
            }
        }
    }

    class FooterHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}