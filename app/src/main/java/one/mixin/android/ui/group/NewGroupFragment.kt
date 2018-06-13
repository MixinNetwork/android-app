package one.mixin.android.ui.group

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tbruyelle.rxpermissions2.RxPermissions
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.fragment_new_group.*
import kotlinx.android.synthetic.main.item_contact_normal.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.REQUEST_IMAGE
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.loadCircleImage
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.openImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.contacts.ProfileFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.Session
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import org.jetbrains.anko.support.v4.indeterminateProgressDialog
import org.jetbrains.anko.textColor
import javax.inject.Inject

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

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val groupViewModel: GroupViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(GroupViewModel::class.java)
    }
    private val sender: User by lazy { Session.getAccount()!!.toUser() }
    private val imageUri: Uri by lazy {
        Uri.fromFile(context?.getImagePath()?.createImageTemp())
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val users: List<User> = arguments!!.getParcelableArrayList(ARGS_USERS)
        title_view.left_ib.setOnClickListener {
            name_desc_et.hideKeyboard()
            activity?.onBackPressed()
        }
        title_view.right_animator.setOnClickListener {
            createGroup()
        }
        enableCreate(false)
        photo_rl.setOnClickListener {
            RxPermissions(activity!!)
                .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
        user_rv.addItemDecoration(SpaceItemDecoration())
        name_desc_et.addTextChangedListener(mWatcher)
        name_desc_et.showKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE) {
            var selectedImageUri: Uri?
            if (data == null || data.action != null &&
                data.action == android.provider.MediaStore.ACTION_IMAGE_CAPTURE) {
                selectedImageUri = imageUri
            } else {
                selectedImageUri = data.data
                if (selectedImageUri == null) {
                    selectedImageUri = imageUri
                }
            }
            val options = UCrop.Options()
            options.setToolbarColor(ContextCompat.getColor(context!!, R.color.black))
            options.setStatusBarColor(ContextCompat.getColor(context!!, R.color.black))
            options.setHideBottomControls(true)
            UCrop.of(selectedImageUri, imageUri)
                .withOptions(options)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(ProfileFragment.MAX_PHOTO_SIZE, ProfileFragment.MAX_PHOTO_SIZE)
                .start(activity!!)
        }
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null) {
                resultUri = UCrop.getOutput(data)
                new_group_avatar.loadCircleImage(resultUri.toString(), R.drawable.ic_photo_camera)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                context?.toast("crop failed")
            }
        }
    }

    private fun createGroup() {
        val groupIcon = if (resultUri == null) {
            null
        } else {
            val bitmap = MediaStore.Images.Media.getBitmap(context!!.contentResolver, resultUri)
            Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP)
        }
        val conversation = groupViewModel.createGroupConversation(name_desc_et.text.toString(),
            notice_desc_et.text.toString(), groupIcon, adapter.users!!, sender)

        val liveData = groupViewModel.getConversationStatusById(conversation.conversationId)
        liveData.observe(this, Observer { c ->
            if (c != null) {
                when {
                    c.status == ConversationStatus.START.ordinal -> {
                        if (dialog == null) {
                            dialog = indeterminateProgressDialog(message = R.string.pb_dialog_message,
                                title = R.string.group_creating).apply {
                                setCancelable(false)
                            }
                        }
                        dialog?.show()
                    }
                    c.status == ConversationStatus.SUCCESS.ordinal -> {
                        liveData.removeObservers(this)
                        name_desc_et.hideKeyboard()
                        dialog?.dismiss()
                        startActivity(Intent(context, MainActivity::class.java))
                        ConversationActivity.show(context!!, conversation.conversationId, null)
                    }
                    c.status == ConversationStatus.FAILURE.ordinal -> {
                        name_desc_et.hideKeyboard()
                        dialog?.dismiss()
                        startActivity(Intent(context, MainActivity::class.java))
                    }
                }
            }
        })
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

        override fun getItemCount(): Int = notNullElse(users, { it.size }, 0)
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: User) {
            itemView.avatar.setInfo(if (user.fullName != null && user.fullName.isNotEmpty()) user.fullName[0] else ' ',
                user.avatarUrl, user.identityNumber)
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