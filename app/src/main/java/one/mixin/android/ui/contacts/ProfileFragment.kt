package one.mixin.android.ui.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Dialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.text.InputType
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDisposable
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.extension.REQUEST_IMAGE
import one.mixin.android.extension.alert
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.openImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import org.jetbrains.anko.dimen
import org.jetbrains.anko.margin
import org.jetbrains.anko.noButton
import org.jetbrains.anko.singleLine
import javax.inject.Inject

class ProfileFragment : BaseFragment() {

    companion object {
        const val TAG = "ProfileFragment"

        const val POS_CONTENT = 0
        const val POS_PROGRESS = 1

        const val MAX_PHOTO_SIZE = 512

        fun newInstance() = ProfileFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val contactsViewModel: ContactViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ContactViewModel::class.java)
    }
    private var user: User? = null
    private val imageUri: Uri by lazy {
        Uri.fromFile(requireContext().getImagePath().createImageTemp())
    }
    private var dialog: Dialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        val account = Session.getAccount()
        if (account != null) {
            name_desc_tv.text = account.full_name
            phone_desc_tv.text = account.phone

            name_rl.setOnClickListener { showDialog(false) }
            phone_rl.setOnClickListener {
                alert(getString(R.string.profile_modify_number)) {
                    positiveButton(R.string.profile_phone) { dialog ->
                        dialog.dismiss()
                        activity?.supportFragmentManager?.inTransaction {
                            setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                                R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                                .add(R.id.container, VerifyFragment.newInstance())
                                .addToBackStack(null)
                        }
                    }
                    noButton { dialog -> dialog.dismiss() }
                }.show()
            }
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
        }
        redeem_rl.setOnClickListener { showDialog(true) }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        contactsViewModel.findSelf().observe(this, Observer { self ->
            if (self != null) {
                user = self
                name_desc_tv.text = self.fullName
                phone_desc_tv.text = self.phone
                profile_avatar.setInfo(self.fullName, self.avatarUrl, self.userId)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_IMAGE) {
            var selectedImageUri: Uri?
            if (data == null || data.action != null && data.action == MediaStore.ACTION_IMAGE_CAPTURE) {
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
                .withMaxResultSize(MAX_PHOTO_SIZE, MAX_PHOTO_SIZE)
                .start(activity!!)
        }
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null && context != null) {
                val resultUri = UCrop.getOutput(data)
                val bitmap = MediaStore.Images.Media.getBitmap(context!!.contentResolver, resultUri)
                update(Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP), true)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val cropError = UCrop.getError(data)
                context?.toast(cropError.toString())
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showDialog(isRedeem: Boolean) {
        if (context == null) {
            return
        }
        val editText = EditText(context!!)
        editText.singleLine = true
        if (!isRedeem) {
            editText.hint = getString(R.string.profile_modify_name_hint)
            val text = name_desc_tv.text.toString()
            editText.setText(text)
            editText.setSelection(text.length)
        } else {
            editText.hint = getString(R.string.wallet_redeem)
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        val frameLayout = FrameLayout(requireContext())
        frameLayout.addView(editText)
        val params = editText.layoutParams as FrameLayout.LayoutParams
        params.margin = context!!.dimen(R.dimen.activity_horizontal_margin)
        editText.layoutParams = params
        dialog = AlertDialog.Builder(context!!, R.style.MixinAlertDialogTheme)
            .setTitle(if (isRedeem) R.string.wallet_get_free_redeem else R.string.profile_modify_name)
            .setView(frameLayout)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(if (isRedeem) R.string.wallet_redeem else R.string.confirm) { dialog, _ ->
                if (isRedeem) {
                    redeem(editText.text.toString())
                } else {
                    update(editText.text.toString(), false)
                    dialog.dismiss()
                }
            }
            .show()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun redeem(code: String) {
        if (code.isEmpty()) {
            context?.toast(R.string.can_not_empty)
            return
        }
        dialog?.dismiss()
        redeem_pb.visibility = VISIBLE
        contactsViewModel.redeem(code).autoDisposable(scopeProvider).subscribe({ r: MixinResponse<Account> ->
            redeem_pb.visibility = GONE
            if (r.isSuccess) {
                r.data?.let {
                    Session.storeAccount(it)
                    contactsViewModel.insertUser(it.toUser())
//                    renderInvitation(it)
                }
            } else {
                ErrorHandler.handleMixinError(r.errorCode)
            }
        }, { t: Throwable ->
            redeem_pb.visibility = GONE
            ErrorHandler.handleError(t)
        })
    }

    @Suppress("unused")
    private fun renderInvitation(account: Account) {
        if (account.invitation_code.isEmpty()) {
            invitation_rl.visibility = GONE
            redeem_rl.visibility = VISIBLE
            invitation_count_tv.text = getString(R.string.wallet_get_free_redeem_tip)
        } else {
            invitation_rl.visibility = VISIBLE
            redeem_rl.visibility = GONE
            invitation_desc_tv.text = account.invitation_code
            invitation_count_tv.text = getString(R.string.profile_invitation_tip, account.consumed_count)
        }
    }

    private fun update(content: String, isPhoto: Boolean) {
        if (isAdded && isPhoto) {
            photo_animator.displayedChild = POS_PROGRESS
        } else {
            name_animator.displayedChild = POS_PROGRESS
        }
        val accountUpdateRequest = if (isPhoto) {
            AccountUpdateRequest(null, content)
        } else {
            AccountUpdateRequest(content, null)
        }
        contactsViewModel.update(accountUpdateRequest)
            .autoDisposable(scopeProvider).subscribe({ r: MixinResponse<Account> ->
                if (isAdded && isPhoto) {
                    photo_animator.displayedChild = POS_CONTENT
                } else {
                    name_animator.displayedChild = POS_CONTENT
                }
                if (!r.isSuccess) {
                    ErrorHandler.handleMixinError(r.errorCode)
                    return@subscribe
                }
                r.data?.let { data ->
                    Session.storeAccount(data)
                    contactsViewModel.insertUser(data.toUser())
                }
            }, { t: Throwable ->
                if (isAdded && isPhoto) {
                    photo_animator.displayedChild = POS_CONTENT
                } else {
                    name_animator.displayedChild = POS_CONTENT
                }
                ErrorHandler.handleError(t)
            })
    }
}