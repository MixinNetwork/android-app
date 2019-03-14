package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_blocked.*
import kotlinx.android.synthetic.main.item_contact_normal.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
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
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        settingBlockedViewModel.blockingUsers(scopeProvider).observe(this, Observer {
            if (it != null && it.isNotEmpty()) {
                block_va.displayedChild = POS_LIST
                adapter.submitList(it)
            } else {
                block_va.displayedChild = POS_EMPTY
            }
        })
    }

    class BlockedAdapter : ListAdapter<User, ItemHolder>(User.DIFF_CALLBACK) {
        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_contact_normal, parent, false))
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: User) {
            itemView.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            itemView.normal.text = user.fullName
            itemView.setOnClickListener {
                UserBottomSheetDialogFragment.newInstance(user).show(
                    (it.context as FragmentActivity).supportFragmentManager, UserBottomSheetDialogFragment.TAG)
            }
        }
    }
}