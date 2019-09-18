package one.mixin.android.ui.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
import one.mixin.android.ui.common.EditBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.common.VerifyFragment.Companion.FROM_PHONE
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import org.jetbrains.anko.noButton
import javax.inject.Inject

class ProfileFragment : BaseFragment() {

    companion object {
        const val TAG = "ProfileFragment"

        const val POS_CONTENT = 0
        const val POS_PROGRESS = 1

        const val TYPE_NAME = 0
        const val TYPE_PHOTO = 1
        const val TYPE_BIOGRAPHY = 2

        const val MAX_PHOTO_SIZE = 512

        fun newInstance() = ProfileFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val contactsViewModel: ContactViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ContactViewModel::class.java)
    }
    private var user: User? = null
    private val imageUri: Uri by lazy {
        Uri.fromFile(requireContext().getImagePath().createImageTemp())
    }
    private var dialog: Dialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_profile, container, false)

    @SuppressLint("AutoDispose")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        val account = Session.getAccount()
        if (account != null) {
            name_desc_tv.text = account.full_name
            phone_desc_tv.text = account.phone
            biography_desc_tv.text = account.biography
            name_rl.setOnClickListener { editName() }
            biography_rl.setOnClickListener { editBiography() }
            phone_rl.setOnClickListener {
                alert(getString(R.string.profile_modify_number)) {
                    positiveButton(R.string.profile_phone) { dialog ->
                        dialog.dismiss()
                        if (Session.getAccount()?.hasPin == true) {
                            activity?.supportFragmentManager?.inTransaction {
                                setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom,
                                    R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                                    .add(R.id.container, VerifyFragment.newInstance(FROM_PHONE))
                                    .addToBackStack(null)
                            }
                        } else {
                            activity?.supportFragmentManager?.inTransaction {
                                setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R
                                    .anim.slide_in_bottom, R.anim.slide_out_bottom)
                                    .add(R.id.container, WalletPasswordFragment.newInstance(), WalletPasswordFragment.TAG)
                                    .addToBackStack(null)
                            }
                        }
                    }
                    noButton { dialog -> dialog.dismiss() }
                }.show()
            }
            photo_rl.setOnClickListener {
                RxPermissions(activity!!)
                    .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDisposable(stopScope)
                    .subscribe { granted ->
                        if (granted) {
                            openImage(imageUri)
                        } else {
                            context?.openPermissionSetting()
                        }
                    }
            }
        }
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
            options.setToolbarWidgetColor(Color.WHITE)
            options.setHideBottomControls(true)
            UCrop.of(selectedImageUri, imageUri)
                .withOptions(options)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(MAX_PHOTO_SIZE, MAX_PHOTO_SIZE)
                .start(requireContext(), this)
        }
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null && context != null) {
                val resultUri = UCrop.getOutput(data)
                val bitmap = MediaStore.Images.Media.getBitmap(context!!.contentResolver, resultUri)
                update(Base64.encodeToString(bitmap.toBytes(), Base64.NO_WRAP), TYPE_PHOTO)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            if (data != null) {
                val cropError = UCrop.getError(data)
                context?.toast(cropError.toString())
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun editBiography() {
        if (context == null) {
            return
        }
        val biographyFragment = EditBottomSheetDialogFragment.newInstance(false)
        biographyFragment.changeAction = {
            update(it, TYPE_BIOGRAPHY)
        }
        biographyFragment.show(fragmentManager, EditBottomSheetDialogFragment.TAG)
    }

    @SuppressLint("RestrictedApi")
    private fun editName() {
        if (context == null) {
            return
        }
        val biographyFragment = EditBottomSheetDialogFragment.newInstance(true)
        biographyFragment.changeAction = {
            update(it, TYPE_NAME)
        }
        biographyFragment.show(fragmentManager, EditBottomSheetDialogFragment.TAG)
    }

    @Suppress("unused")
    private fun renderInvitation(account: Account) {
        if (account.invitation_code.isEmpty()) {
            invitation_rl.visibility = GONE
            invitation_count_tv.text = getString(R.string.wallet_get_free_redeem_tip)
        } else {
            invitation_rl.visibility = VISIBLE
            invitation_desc_tv.text = account.invitation_code
            invitation_count_tv.text = getString(R.string.profile_invitation_tip, account.consumed_count)
        }
    }

    private fun update(content: String, type: Int) {
        if (!isAdded) return

        when (type) {
            TYPE_PHOTO -> photo_animator.displayedChild = POS_PROGRESS
            TYPE_BIOGRAPHY -> biography_animator.displayedChild = POS_PROGRESS
            else -> name_animator.displayedChild = POS_PROGRESS
        }
        val accountUpdateRequest = when (type) {
            TYPE_PHOTO -> AccountUpdateRequest(null, content)
            TYPE_BIOGRAPHY -> AccountUpdateRequest(biography = content)
            else -> AccountUpdateRequest(content, null)
        }
        contactsViewModel.update(accountUpdateRequest)
            .autoDisposable(stopScope).subscribe({ r: MixinResponse<Account> ->
                if (!isAdded) return@subscribe
                when (type) {
                    TYPE_PHOTO -> photo_animator.displayedChild = POS_CONTENT
                    TYPE_BIOGRAPHY -> biography_animator.displayedChild = POS_CONTENT
                    else -> name_animator.displayedChild = POS_CONTENT
                }
                if (!r.isSuccess) {
                    ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                    return@subscribe
                }
                r.data?.let { data ->
                    Session.storeAccount(data)
                    contactsViewModel.insertUser(data.toUser())
                }
                if (type == TYPE_BIOGRAPHY) {
                    Session.storeAccount(r.data!!)
                    biography_desc_tv.text = r.data!!.biography
                }
            }, { t: Throwable ->
                if (!isAdded) return@subscribe

                when (type) {
                    TYPE_PHOTO -> photo_animator.displayedChild = POS_CONTENT
                    TYPE_BIOGRAPHY -> biography_animator.displayedChild = POS_CONTENT
                    else -> name_animator.displayedChild = POS_CONTENT
                }
                ErrorHandler.handleError(t)
            })
    }
}
