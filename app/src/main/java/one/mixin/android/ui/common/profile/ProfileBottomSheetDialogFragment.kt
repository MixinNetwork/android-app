package one.mixin.android.ui.common.profile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.fragment_profile_bottom_sheet_dialog.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.extension.REQUEST_CAMERA
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alert
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.dayTime
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.openCamera
import one.mixin.android.extension.openGallery
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.common.editDialog
import one.mixin.android.ui.common.info.MixinScrollableBottomSheetDialogFragment
import one.mixin.android.ui.common.info.createMenuLayout
import one.mixin.android.ui.common.info.menuList
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.ui.url.openUrlWithExtraWeb
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Account
import one.mixin.android.vo.App
import one.mixin.android.vo.toUser
import one.mixin.android.widget.linktext.AutoLinkMode

class ProfileBottomSheetDialogFragment : MixinScrollableBottomSheetDialogFragment() {
    companion object {
        const val TAG = "ProfileBottomSheetDialogFragment"

        const val TYPE_NAME = 0
        const val TYPE_PHOTO = 1
        const val TYPE_BIOGRAPHY = 2

        const val MAX_PHOTO_SIZE = 512

        fun newInstance() =
            ProfileBottomSheetDialogFragment()
    }

    private val imageUri: Uri by lazy {
        Uri.fromFile(requireContext().getImagePath().createImageTemp())
    }

    private var menuListLayout: ViewGroup? = null

    override fun getLayoutId() = R.layout.fragment_profile_bottom_sheet_dialog

    override fun getPeekHeight(contentView: View, behavior: BottomSheetBehavior<*>): Int {
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(contentView.width, View.MeasureSpec.EXACTLY),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        behavior.skipCollapsed = true
        return contentView.measuredHeight
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title.right_iv.setOnClickListener { dismiss() }
        val account = Session.getAccount()
        if (account == null) {
            toast(R.string.error_user_invalid_format)
            return
        }
        contentView.apply {
            detail_tv.movementMethod = LinkMovementMethod()
            detail_tv.addAutoLinkMode(AutoLinkMode.MODE_URL)
            detail_tv.setUrlModeColor(BaseViewHolder.LINK_COLOR)
            detail_tv.setAutoLinkOnClickListener { _, url ->
                openUrlWithExtraWeb(url, null, parentFragmentManager)
                dismiss()
            }
            created_tv.text = getString(R.string.profile_join_in, account.created_at.dayTime())
            refreshInfo(account)
        }

        lifecycleScope.launch {
            try {
                bottomViewModel.loadFavoriteApps((account.userId)) {
                    initMenu(account, it)
                }
            } catch (e: Exception) {
                ErrorHandler.handleError(e)
            }
        }
    }

    private fun refreshInfo(account: Account) {
        contentView.apply {
            name.text = account.full_name
            avatar.setInfo(account.full_name, account.avatar_url, account.userId)
            id_tv.text = getString(R.string.contact_mixin_id, account.identity_number)
            detail_tv.text = account.biography
        }
    }

    private fun initMenu(account: Account, favoriteApps: List<App>?) {
        val list = menuList {
            menuGroup {
                menu {
                    title = getString(R.string.contact_my_share_apps)
                    action = {
                        activity?.addFragment(
                            this@ProfileBottomSheetDialogFragment,
                            MySharedAppsFragment.newInstance(),
                            MySharedAppsFragment.TAG
                        )
                        dismiss()
                    }
                    apps = favoriteApps
                }
            }
            menuGroup {
                menu {
                    title = getString(R.string.contact_my_qr_title)
                    action = {
                        QrBottomSheetDialogFragment.newInstance(
                            account.userId,
                            QrBottomSheetDialogFragment.TYPE_MY_QR
                        ).showNow(parentFragmentManager, QrBottomSheetDialogFragment.TAG)
                    }
                }
                menu {
                    title = getString(R.string.contact_receive_money)
                    action = {
                        QrBottomSheetDialogFragment.newInstance(
                            account.userId,
                            QrBottomSheetDialogFragment.TYPE_RECEIVE_QR
                        ).showNow(parentFragmentManager, QrBottomSheetDialogFragment.TAG)
                    }
                }
            }
            menuGroup {
                menu {
                    title = getString(R.string.edit_name)
                    action = { editName() }
                }
                menu {
                    title = getString(R.string.edit_biography)
                    action = { editBiography() }
                }
            }
            menuGroup {
                menu {
                    title = getString(R.string.change_profile_photo_with_camera)
                    action = { changePhoto(true) }
                }
                menu {
                    title = getString(R.string.change_profile_photo_with_Library)
                    action = { changePhoto(false) }
                }
            }
            menuGroup {
                menu {
                    title = getString(R.string.profile_phone)
                    subtitle = account.phone
                    action = { changeNumber() }
                }
            }
        }

        menuListLayout?.removeAllViews()
        list.createMenuLayout(requireContext()).let { layout ->
            menuListLayout = layout
            contentView.scroll_content.addView(layout, contentView.scroll_content.childCount - 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ((requestCode == REQUEST_GALLERY || requestCode == REQUEST_CAMERA) && resultCode == Activity.RESULT_OK) {
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
            options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.black))
            options.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.black))
            options.setToolbarWidgetColor(Color.WHITE)
            options.setHideBottomControls(true)
            UCrop.of(selectedImageUri, imageUri)
                .withOptions(options)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(
                    MAX_PHOTO_SIZE,
                    MAX_PHOTO_SIZE
                )
                .start(requireContext(), this)
        }
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null && context != null) {
                val resultUri = UCrop.getOutput(data)
                val bitmap =
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, resultUri)
                update(
                    Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP),
                    TYPE_PHOTO
                )
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val cropError = UCrop.getError(data)
                context?.toast(cropError.toString())
            }
        }
    }

    private fun changePhoto(byCamera: Boolean) {
        RxPermissions(activity!!)
            .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    if (byCamera) {
                        openCamera(imageUri)
                    } else {
                        openGallery(true)
                    }
                } else {
                    context?.openPermissionSetting()
                }
            }
    }

    private fun changeNumber() {
        alert(getString(R.string.profile_modify_number))
            .setNegativeButton(android.R.string.no) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.profile_phone) { dialog, _ ->
                dialog.dismiss()
                if (Session.getAccount()?.hasPin == true) {
                    activity?.supportFragmentManager?.inTransaction {
                        setCustomAnimations(
                            R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                            R.anim.slide_in_bottom, R.anim.slide_out_bottom
                        )
                            .add(
                                R.id.container,
                                VerifyFragment.newInstance(VerifyFragment.FROM_PHONE)
                            )
                            .addToBackStack(null)
                    }
                } else {
                    activity?.supportFragmentManager?.inTransaction {
                        setCustomAnimations(
                            R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                            R.anim.slide_in_bottom, R.anim.slide_out_bottom
                        )
                            .add(
                                R.id.container,
                                WalletPasswordFragment.newInstance(),
                                WalletPasswordFragment.TAG
                            )
                            .addToBackStack(null)
                    }
                }
                dismiss()
            }.show()
    }

    @SuppressLint("RestrictedApi")
    private fun editName() {
        if (context == null) {
            return
        }
        editDialog {
            titleText = this@ProfileBottomSheetDialogFragment.getString(R.string.edit_name)
            editText = Session.getAccount()?.full_name
            maxTextCount = 40
            allowEmpty = false
            rightAction = {
                update(it, TYPE_NAME)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun editBiography() {
        if (context == null) {
            return
        }
        editDialog {
            titleText = this@ProfileBottomSheetDialogFragment.getString(R.string.edit_biography)
            editText = Session.getAccount()?.biography
            maxTextCount = 140
            allowEmpty = true
            rightAction = {
                update(it, TYPE_BIOGRAPHY)
            }
        }
    }

    private fun update(content: String, type: Int) {
        if (!isAdded) return

        val accountUpdateRequest = when (type) {
            TYPE_PHOTO -> AccountUpdateRequest(null, content)
            TYPE_BIOGRAPHY -> AccountUpdateRequest(biography = content)
            else -> AccountUpdateRequest(content, null)
        }
        bottomViewModel.update(accountUpdateRequest)
            .autoDispose(stopScope).subscribe({ r: MixinResponse<Account> ->
                if (!isAdded) return@subscribe
                if (!r.isSuccess) {
                    ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                    return@subscribe
                }
                r.data?.let { data ->
                    Session.storeAccount(data)
                    bottomViewModel.insertUser(data.toUser())
                    refreshInfo(data)
                }
            }, { t: Throwable ->
                ErrorHandler.handleError(t)
            })
    }
}
