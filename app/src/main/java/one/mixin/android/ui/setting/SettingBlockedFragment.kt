package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentBlockedBinding
import one.mixin.android.databinding.ItemBlockedFooterBinding
import one.mixin.android.databinding.ItemContactNormalBinding
import one.mixin.android.databinding.ViewTitleBinding
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.vo.User

@AndroidEntryPoint
class SettingBlockedFragment : BaseSettingFragment<FragmentBlockedBinding>() {
    companion object {
        const val TAG = "SettingBlockedFragment"
        const val POS_LIST = 0
        const val POS_EMPTY = 1

        fun newInstance(): SettingBlockedFragment {
            return SettingBlockedFragment()
        }
    }

    private val viewModel by viewModels<SettingBlockedViewModel>()

    private val adapter = BlockedAdapter()

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentBlockedBinding.inflate(inflater, container, false).apply {
            _titleBinding = ViewTitleBinding.bind(titleView)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.callback = object : Callback {
            override fun onClick(user: User) {
                UserBottomSheetDialogFragment.newInstance(user)
                    .show(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
            }
        }
        binding.apply {
            blockedRv.adapter = adapter
            titleBinding.leftIb.setOnClickListener { activity?.onBackPressed() }
            viewModel.blockingUsers(stopScope).observe(
                viewLifecycleOwner,
                {
                    if (it != null && it.isNotEmpty()) {
                        blockVa.displayedChild = POS_LIST
                        adapter.setUsers(it)
                    } else {
                        blockVa.displayedChild = POS_EMPTY
                    }
                }
            )
        }
    }

    class BlockedAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object {
            const val TYPE_NORMAL = 0
            const val TYPE_FOOTER = 1
        }

        private var users: List<User>? = null

        var callback: Callback? = null

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
                FooterHolder(ItemBlockedFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false).root)
            } else {
                ItemHolder(ItemContactNormalBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (users == null || users!!.isEmpty()) {
                return
            }
            if (holder is ItemHolder) {
                holder.bind(users!![position], callback)
            }
        }

        override fun getItemCount(): Int = (users?.size ?: 0) + 1
    }

    class ItemHolder(private val itemBinding: ItemContactNormalBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(user: User, callback: Callback?) {
            itemBinding.apply {
                avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
                normal.text = user.fullName
            }
            itemView.setOnClickListener {
                callback?.onClick(user)
            }
        }
    }

    class FooterHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface Callback {
        fun onClick(user: User)
    }
}
