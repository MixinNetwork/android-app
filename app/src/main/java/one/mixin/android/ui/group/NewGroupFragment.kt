package one.mixin.android.ui.group

import android.Manifest
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_new_group.*
import kotlinx.android.synthetic.main.item_contact_normal.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import org.jetbrains.anko.textColor

@AndroidEntryPoint
class NewGroupFragment : BaseFragment() {
    companion object {
        const val TAG = "NewGroupFragment"
        private const val ARGS_USERS = "args_users"

        fun newInstance(users: ArrayList<User>): NewGroupFragment {
            val fragment = NewGroupFragment()
            fragment.withArgs {
                putParcelableArrayList(ARGS_USERS, users)
            }
            return fragment
        }
    }

    private val groupViewModel by viewModels<GroupViewModel>()
    private val sender: User by lazy { Session.getAccount()!!.toUser() }
    private val imageUri: Uri by lazy {
        Uri.fromFile(context?.getOtherPath()?.createImageTemp())
    }
    private var resultUri: Uri? = null
    private val adapter = NewGroupAdapter()
    private var dialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_new_group, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val users: List<User> = requireArguments().getParcelableArrayList(ARGS_USERS)!!
        title_view.left_ib.setOnClickListener {
            name_desc_et.hideKeyboard()
            activity?.onBackPressed()
        }
        title_view.right_animator.setOnClickListener {
            createGroup()
        }
        enableCreate(false)
        photo_rl.setOnClickListener {
            RxPermissions(requireActivity())
                .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        openImage(imageUri)
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        }
        adapter.users = users
        user_rv.adapter = adapter
        name_desc_et.addTextChangedListener(mWatcher)
        name_desc_et.showKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.dismiss()
    }

    private fun createGroup() = lifecycleScope.launch {
        if (dialog == null) {
            dialog = indeterminateProgressDialog(
                message = R.string.pb_dialog_message,
                title = R.string.group_creating
            ).apply {
                setCancelable(false)
            }
        }
        dialog?.show()

        val groupIcon = if (resultUri == null) {
            null
        } else {
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, resultUri)
            Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP)
        }
        val conversation = groupViewModel.createGroupConversation(
            name_desc_et.text.toString(),
            notice_desc_et.text.toString(),
            groupIcon,
            adapter.users!!,
            sender
        )
        val liveData = groupViewModel.getConversationStatusById(conversation.conversationId)
        liveData.observe(
            viewLifecycleOwner,
            { c ->
                if (c != null) {
                    when (c.status) {
                        ConversationStatus.SUCCESS.ordinal -> {
                            liveData.removeObservers(viewLifecycleOwner)
                            name_desc_et.hideKeyboard()
                            dialog?.dismiss()
                            activity?.finish()
                            ConversationActivity.showAndClear(requireContext(), conversation.conversationId)
                        }
                        ConversationStatus.FAILURE.ordinal -> {
                            liveData.removeObservers(viewLifecycleOwner)
                            name_desc_et.hideKeyboard()
                            dialog?.dismiss()
                            MainActivity.reopen(requireContext())
                        }
                    }
                }
            }
        )
    }

    private fun enableCreate(enable: Boolean) {
        if (enable) {
            title_view.right_tv.textColor = resources.getColor(R.color.colorBlue, null)
            title_view.right_animator.isEnabled = true
        } else {
            title_view.right_tv.textColor = resources.getColor(R.color.text_gray, null)
            title_view.right_animator.isEnabled = false
        }
    }

    class NewGroupAdapter : RecyclerView.Adapter<ItemHolder>() {
        var users: List<User>? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_contact_normal, parent, false))

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            if (users == null || users!!.isEmpty()) {
                return
            }
            holder.bind(users!![position])
        }

        override fun getItemCount(): Int = users?.size ?: 0
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: User) {
            itemView.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            itemView.normal.text = user.fullName
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (!s.isNullOrEmpty()) {
                enableCreate(true)
            } else {
                enableCreate(false)
            }
        }
    }
}
