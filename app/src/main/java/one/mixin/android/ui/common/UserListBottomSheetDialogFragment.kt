package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_TITLE
import one.mixin.android.databinding.FragmentUserListBottomSheetBinding
import one.mixin.android.databinding.ItemUserListBinding
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.withArgs
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class UserListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "UserListBottomSheetDialogFragment"
        const val ARGS_USER_LIST = "args_user_list"

        fun newInstance(
            userList: ArrayList<User>,
            title: String,
        ) = UserListBottomSheetDialogFragment().withArgs {
            putParcelableArrayList(ARGS_USER_LIST, userList)
            putString(ARGS_TITLE, title)
        }
    }

    private val userList by lazy {
        requireArguments().getParcelableArrayListCompat(ARGS_USER_LIST, User::class.java)
    }
    private val title: String by lazy {
        requireArguments().getString(ARGS_TITLE)!!
    }

    private val adapter = UserListAdapter()

    private val binding by viewBinding(FragmentUserListBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            titleView.rightIv.setOnClickListener { dismiss() }
            titleView.titleTv.text = title
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
        }
        adapter.submitList(userList)
    }
}

class UserListAdapter : ListAdapter<User, UserHolder>(User.DIFF_CALLBACK) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) =
        UserHolder(ItemUserListBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(
        holder: UserHolder,
        position: Int,
    ) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }
}

class UserHolder(val binding: ItemUserListBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(user: User) {
        binding.nameTv.setName(user)
        binding.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
        binding.numberTv.text = user.identityNumber
    }
}
