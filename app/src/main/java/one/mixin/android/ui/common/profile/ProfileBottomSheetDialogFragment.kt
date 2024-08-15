package one.mixin.android.ui.common.profile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.uber.autodispose.autoDispose
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.Colors.LINK_COLOR
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.databinding.FragmentProfileBottomSheetDialogBinding
import one.mixin.android.extension.REQUEST_CAMERA
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.alert
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.dayTime
import one.mixin.android.extension.getCapturedImage
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.openCamera
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.AvatarActivity
import one.mixin.android.ui.common.EditDialog
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.common.editDialog
import one.mixin.android.ui.common.info.MixinScrollableBottomSheetDialogFragment
import one.mixin.android.ui.common.info.createMenuLayout
import one.mixin.android.ui.common.info.menuList
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipType
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.Account
import one.mixin.android.vo.App
import one.mixin.android.vo.membershipIcon
import one.mixin.android.vo.showVerifiedOrBot
import one.mixin.android.vo.toUser
import one.mixin.android.widget.linktext.AutoLinkMode
import timber.log.Timber

@AndroidEntryPoint
class ProfileBottomSheetDialogFragment : MixinScrollableBottomSheetDialogFragment() {
    companion object {
        const val TAG = "ProfileBottomSheetDialogFragment"

        const val TYPE_NAME = 0
        const val TYPE_PHOTO = 1
        const val TYPE_BIOGRAPHY = 2

        const val MAX_PHOTO_SIZE = 512

        @SuppressLint("StaticFieldLeak")
        private var instant: ProfileBottomSheetDialogFragment? = null

        fun newInstance(): ProfileBottomSheetDialogFragment {
            try {
                instant?.dismiss()
            } catch (ignored: IllegalStateException) {
            }
            instant = null
            return ProfileBottomSheetDialogFragment().apply {
                instant = this
            }
        }
    }

    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        pickMedia =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia(), requireActivity().activityResultRegistry) { uri ->
                if (uri != null) {
                    val options = UCrop.Options()
                    options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.black))
                    options.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.black))
                    options.setToolbarWidgetColor(Color.WHITE)
                    options.setHideBottomControls(true)
                    UCrop.of(uri, imageUri)
                        .withOptions(options)
                        .withAspectRatio(1f, 1f)
                        .withMaxResultSize(
                            MAX_PHOTO_SIZE,
                            MAX_PHOTO_SIZE,
                        )
                        .start(requireContext(), this)
                } else {
                    Timber.e("PhotoPicker, No media selected")
                }
            }
    }

    override fun onDetach() {
        super.onDetach()
        instant = null
    }

    private val imageUri: Uri by lazy {
        Uri.fromFile(requireContext().getOtherPath().createImageTemp())
    }

    private var menuListLayout: ViewGroup? = null

    override fun getLayoutId() = R.layout.fragment_profile_bottom_sheet_dialog

    private val binding by lazy {
        FragmentProfileBottomSheetDialogBinding.bind(contentView)
    }

    override fun getPeekHeight(
        contentView: View,
        behavior: BottomSheetBehavior<*>,
    ): Int {
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(contentView.width, View.MeasureSpec.EXACTLY),
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        behavior.skipCollapsed = true
        return contentView.measuredHeight
    }

    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        binding.title.rightIv.setOnClickListener { dismiss() }
        val account = Session.getAccount()
        if (account == null) {
            toast(R.string.error_user_invalid_format)
            return
        }
        binding.apply {
            detailTv.movementMethod = LinkMovementMethod()
            detailTv.addAutoLinkMode(AutoLinkMode.MODE_URL)
            detailTv.setUrlModeColor(LINK_COLOR)
            detailTv.setAutoLinkOnClickListener { _, url ->
                url.openAsUrlOrWeb(requireContext(), null, parentFragmentManager, lifecycleScope)
                dismiss()
            }

            createdTv.text = getString(R.string.Joined_in, account.createdAt.dayTime())

            bottomViewModel.loadFavoriteApps(account.userId)
            bottomViewModel.observerFavoriteApps(account.userId)
                .observe(this@ProfileBottomSheetDialogFragment) { apps ->
                    initMenu(account, apps)
                }
        }

        bottomViewModel.observeSelf().observe(this@ProfileBottomSheetDialogFragment) {
            Session.getAccount()?.let { refreshInfo(it) }
        }

        bottomViewModel.refreshAccount()
    }

    private fun refreshInfo(account: Account) {
        binding.apply {
            avatar.setOnClickListener {
                if (!isAdded) return@setOnClickListener

                val avatar = account.avatarUrl
                if (avatar.isNullOrBlank()) {
                    return@setOnClickListener
                }
                AvatarActivity.show(requireActivity(), avatar, binding.avatar)
                dismiss()
            }

            name.text = account.fullName
            if (account.membership?.isMembership() == null) {
                binding.membershipIv.isVisible = true
                binding.membershipIv.setImageResource(account.membership.membershipIcon())
            } else {
                binding.membershipIv.isVisible = false
            }
            avatar.setInfo(account.fullName, account.avatarUrl, account.userId)
            idTv.text = getString(R.string.contact_mixin_id, account.identityNumber)
            detailTv.originalText = account.biography ?: ""
        }
    }

    private fun initMenu(
        account: Account,
        favoriteApps: List<App>?,
    ) {
        val list =
            menuList {
                menuGroup {
                    menu {
                        title = getString(R.string.My_favorite_bots)
                        action = {
                            activity?.addFragment(
                                this@ProfileBottomSheetDialogFragment,
                                MySharedAppsFragment.newInstance(),
                                MySharedAppsFragment.TAG,
                            )
                            dismiss()
                        }
                        apps = favoriteApps
                    }
                }
                menuGroup {
                    menu {
                        title = getString(R.string.My_QR_Code)
                        action = {
                            QrBottomSheetDialogFragment.newInstance(
                                account.userId,
                                QrBottomSheetDialogFragment.TYPE_MY_QR,
                            ).showNow(parentFragmentManager, QrBottomSheetDialogFragment.TAG)
                        }
                    }
                    menu {
                        title = getString(R.string.Receive_Money)
                        action = {
                            QrBottomSheetDialogFragment.newInstance(
                                account.userId,
                                QrBottomSheetDialogFragment.TYPE_RECEIVE_QR,
                            ).showNow(parentFragmentManager, QrBottomSheetDialogFragment.TAG)
                        }
                    }
                }
                menuGroup {
                    menu {
                        title = getString(R.string.Edit_Name)
                        action = { editName() }
                    }
                    menu {
                        title = getString(R.string.Edit_Biography)
                        action = { editBiography() }
                    }
                }
                menuGroup {
                    menu {
                        title = getString(R.string.Change_Profile_Photo_with_Camera)
                        action = { changePhoto(true) }
                    }
                    menu {
                        title = getString(R.string.Change_Profile_Photo_with_Library)
                        action = { changePhoto(false) }
                    }
                }
                menuGroup {
                    menu {
                        title = getString(R.string.Change_Phone_Number)
                        subtitle = account.phone
                        action = { changeNumber() }
                    }
                }
            }

        menuListLayout?.removeAllViews()
        list.createMenuLayout(requireContext()).let { layout ->
            menuListLayout = layout
            binding.scrollContent.addView(layout, binding.scrollContent.childCount - 1)
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
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
                    MAX_PHOTO_SIZE,
                )
                .start(requireContext(), this)
        }
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null && context != null) {
                val resultUri = UCrop.getOutput(data)
                val bitmap = resultUri?.getCapturedImage(requireContext().contentResolver)
                update(
                    Base64.encodeToString(bitmap?.toBytes(), Base64.NO_WRAP),
                    TYPE_PHOTO,
                )
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val cropError = UCrop.getError(data)
                toast(cropError.toString())
            }
        }
    }

    private fun changePhoto(byCamera: Boolean) {
        if (!byCamera) {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            RxPermissions(requireActivity())
                .request(
                    *mutableListOf(Manifest.permission.CAMERA).apply {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }.toTypedArray(),
                )
                .autoDispose(stopScope)
                .subscribe { granted ->
                    if (granted) {
                        openCamera(imageUri)
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        }
    }

    private fun changeNumber() {
        alert(getString(R.string.profile_modify_number))
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.Change_Phone_Number) { dialog, _ ->
                dialog.dismiss()
                if (Session.getAccount()?.hasPin == true) {
                    activity?.supportFragmentManager?.inTransaction {
                        setCustomAnimations(
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom,
                            R.anim.slide_in_bottom,
                            R.anim.slide_out_bottom,
                        )
                            .add(
                                R.id.container,
                                VerifyFragment.newInstance(VerifyFragment.FROM_PHONE),
                            )
                            .addToBackStack(null)
                    }
                } else {
                    TipActivity.show(requireActivity(), TipType.Create, true)
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
            titleText = this@ProfileBottomSheetDialogFragment.getString(R.string.Edit_Name)
            editText = Session.getAccount()?.fullName
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
            titleText = this@ProfileBottomSheetDialogFragment.getString(R.string.Edit_Biography)
            editText = Session.getAccount()?.biography
            maxTextCount = 140
            editMaxLines = EditDialog.MAX_LINE.toInt()
            allowEmpty = true
            rightAction = {
                update(it, TYPE_BIOGRAPHY)
            }
        }
    }

    private fun update(
        content: String,
        type: Int,
    ) {
        if (!isAdded) return

        val accountUpdateRequest =
            when (type) {
                TYPE_PHOTO -> AccountUpdateRequest(null, content)
                TYPE_BIOGRAPHY -> AccountUpdateRequest(biography = content)
                else -> AccountUpdateRequest(content, null)
            }
        bottomViewModel.update(accountUpdateRequest)
            .autoDispose(stopScope).subscribe(
                { r: MixinResponse<Account> ->
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
                },
                { t: Throwable ->
                    ErrorHandler.handleError(t)
                },
            )
    }
}
