package one.mixin.android.ui.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.kotlin.autoDisposable
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.fragment_profile_bottom_sheet.view.*
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.extension.REQUEST_IMAGE
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.openImage
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.AvatarActivity
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.dimen
import org.jetbrains.anko.margin
import org.jetbrains.anko.singleLine

class ProfileBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "ProfileBottomSheetDialogFragment"

        fun newInstance() = ProfileBottomSheetDialogFragment()
    }

    private lateinit var user: User
    private lateinit var menu: AlertDialog
    private var keepDialog = false
    private val imageUri: Uri by lazy {
        Uri.fromFile(requireContext().getImagePath().createImageTemp())
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_profile_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val account = Session.getAccount()
        if (account != null) {
            bottomViewModel.findSelf().observe(this, Observer { self ->
                if (self != null) {
                    user = self
                    contentView.name.text = self.fullName
                    contentView.id_tv.text = getString(R.string.contact_mixin_id, account.identity_number)
                    contentView.avatar.setOnClickListener {
                        account.avatar_url.let { url ->
                            if (!url.isNullOrBlank()) {
                                AvatarActivity.show(requireActivity(), url, contentView.avatar)
                                dismiss()
                            }
                        }
                    }
                    contentView.avatar.setInfo(self.fullName, self.avatarUrl, self.identityNumber)
                }
            })
        }
        contentView.edit_fl.setOnClickListener { showDialog() }
        contentView.camera_fl.setOnClickListener { changeAvatar() }
        val choices = mutableListOf<String>()
        choices.add(getString(R.string.change_avatar))
        choices.add(getString(R.string.edit_name))
        choices.add(getString(R.string.profile_phone))
        menu = AlertDialog.Builder(context!!)
            .setItems(choices.toTypedArray()) { _, which ->
                when (choices[which]) {
                    getString(R.string.change_avatar) -> changeAvatar()
                    getString(R.string.edit_name) -> showDialog()
                    getString(R.string.profile_phone) -> showChangeNumber()
                }
            }.create()
        menu.setOnDismissListener {
            if (!keepDialog) {
                dismiss()
            }
        }

        contentView.more_fl.setOnClickListener {
            (dialog as BottomSheet).fakeDismiss()
            menu.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE) {
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
                .withMaxResultSize(ProfileFragment.MAX_PHOTO_SIZE, ProfileFragment.MAX_PHOTO_SIZE)
                .start(activity!!)
        }
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
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

    private fun showChangeNumber() {
        activity?.supportFragmentManager?.inTransaction {
            setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                .add(R.id.container, VerifyFragment.newInstance(), VerifyFragment.TAG)
                .addToBackStack(null)
        }
    }

    @SuppressLint("CheckResult")
    private fun changeAvatar() {
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

    private fun showDialog() {
        if (context == null) {
            return
        }
        val editText = EditText(context!!)
        editText.singleLine = true
        editText.hint = getString(R.string.profile_modify_name_hint)
        val text = contentView.name.text.toString()
        editText.setText(text)
        editText.setSelection(text.length)
        val frameLayout = FrameLayout(requireContext())
        frameLayout.addView(editText)
        val params = editText.layoutParams as FrameLayout.LayoutParams
        params.margin = context!!.dimen(R.dimen.activity_horizontal_margin)
        editText.layoutParams = params
        val dialog = AlertDialog.Builder(context!!, R.style.MixinAlertDialogTheme)
            .setTitle(R.string.profile_modify_name)
            .setView(frameLayout)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                update(editText.text.toString(), false)
                dialog.dismiss()
            }
            .show()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun update(content: String, isPhoto: Boolean) {
        val accountUpdateRequest = if (isPhoto) {
            AccountUpdateRequest(null, content)
        } else {
            AccountUpdateRequest(content, null)
        }
        bottomViewModel.update(accountUpdateRequest)
            .autoDisposable(scopeProvider).subscribe({ r: MixinResponse<Account> ->
                if (!r.isSuccess) {
                    ErrorHandler.handleMixinError(r.errorCode)
                    return@subscribe
                }
                r.data?.let { data ->
                    Session.storeAccount(data)
                    bottomViewModel.insertUser(data.toUser())
                }
            }, { t: Throwable ->
                ErrorHandler.handleError(t)
            })
    }
}