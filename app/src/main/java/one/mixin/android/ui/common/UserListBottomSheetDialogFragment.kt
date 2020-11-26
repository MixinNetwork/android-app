package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_user_list_bottom_sheet.view.*
import one.mixin.android.R
import one.mixin.android.databinding.ItemUserListBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class UserListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "UserListBottomSheetDialogFragment"
        const val ARGS_USER_LIST = "args_user_list"
        const val ARGS_TITLE = "args_title"

        fun newInstance(
            userList: ArrayList<User>,
            title: String
        ) = UserListBottomSheetDialogFragment().withArgs {
            putParcelableArrayList(ARGS_USER_LIST, userList)
            putString(ARGS_TITLE, title)
        }
    }

    private val userList by lazy {
        requireArguments().getParcelableArrayList<User>(ARGS_USER_LIST)
    }
    private val title: String by lazy {
        requireArguments().getString(ARGS_TITLE)!!
    }

    private val adapter = UserListAdapter()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_user_list_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)

        contentView.title_view.rightIv.setOnClickListener { dismiss() }
        contentView.title_view.titleTv.text = title
        contentView.recycler_view.layoutManager = LinearLayoutManager(requireContext())
        contentView.recycler_view.adapter = adapter
        adapter.submitList(userList)
    }
}

class UserListAdapter : ListAdapter<User, UserHolder>(User.DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        UserHolder(ItemUserListBinding.inflate(LayoutInflater.from(parent.context),parent,false))

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }
}

class UserHolder(val binding: ItemUserListBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(user: User) {
        binding.nameTv.text = user.fullName
        binding.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
        binding.numberTv.text = user.identityNumber
    }
}
