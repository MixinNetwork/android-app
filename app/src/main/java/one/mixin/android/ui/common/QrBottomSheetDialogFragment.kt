package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.Constants.MY_QR
import one.mixin.android.Constants.Scheme.TRANSFER
import one.mixin.android.R
import one.mixin.android.databinding.FragmentQrBottomSheetBinding
import one.mixin.android.databinding.ViewQrBottomBinding
import one.mixin.android.extension.capture
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getQRCodePath
import one.mixin.android.extension.isQRCodeFileExists
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.saveQRCode
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.vo.User
import one.mixin.android.widget.BadgeCircleImageView.Companion.END_BOTTOM
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class QrBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "QrBottomSheetDialogFragment"
        const val ARGS_TYPE = "args_type"

        const val TYPE_MY_QR = 0
        const val TYPE_RECEIVE_QR = 1

        fun newInstance(userId: String, type: Int) = QrBottomSheetDialogFragment().apply {
            arguments = bundleOf(
                ARGS_USER_ID to userId,
                ARGS_TYPE to type
            )
        }
    }

    private val userId: String by lazy { requireArguments().getString(ARGS_USER_ID)!! }
    private val type: Int by lazy { requireArguments().getInt(ARGS_TYPE) }

    private var _binding: FragmentQrBottomSheetBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_qr_bottom_sheet, null)
        _binding = FragmentQrBottomSheetBinding.bind(contentView)
        (dialog as BottomSheet).setCustomView(contentView)

        binding.title.leftIb.setOnClickListener { dismiss() }
        binding.title.rightAnimator.setOnClickListener { showBottom() }
        if (type == TYPE_MY_QR) {
            binding.title.titleTv.text = getString(R.string.contact_my_qr_title)
            binding.tipTv.text = getString(R.string.contact_my_qr_tip)
        } else if (type == TYPE_RECEIVE_QR) {
            binding.title.titleTv.text = getString(R.string.contact_receive_money)
            binding.tipTv.text = getString(R.string.contact_receive_tip)
        }
        bottomViewModel.findUserById(userId).observe(
            this,
            { user ->
                if (user == null) {
                    bottomViewModel.refreshUser(userId, true)
                } else {
                    binding.badgeView.bg.setInfo(user.fullName, user.avatarUrl, user.userId)
                    if (type == TYPE_RECEIVE_QR) {
                        binding.badgeView.badge.setImageResource(R.drawable.ic_contacts_receive_blue)
                        binding.badgeView.pos = END_BOTTOM
                    }

                    val name = getName(user)
                    if (requireContext().isQRCodeFileExists(name)) {
                        binding.qr.setImageBitmap(BitmapFactory.decodeFile(requireContext().getQRCodePath(name).absolutePath))
                    } else {
                        binding.qr.post {
                            Observable.create<Bitmap> { e ->
                                val account = Session.getAccount() ?: return@create
                                val code = when (type) {
                                    TYPE_MY_QR -> account.codeUrl
                                    TYPE_RECEIVE_QR -> "$TRANSFER/${user.userId}"
                                    else -> ""
                                }
                                val b = code.generateQRCode(binding.qr.width)
                                if (b != null) {
                                    b.saveQRCode(requireContext(), name)
                                    e.onNext(b)
                                }
                            }.subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .autoDispose(stopScope)
                                .subscribe(
                                    { r ->
                                        binding.qr.setImageBitmap(r)
                                    },
                                    {
                                    }
                                )
                        }
                    }
                }
            }
        )
    }

    private fun getName(user: User): String {
        return when (type) {
            TYPE_MY_QR -> "${BuildConfig.VERSION_CODE}-$MY_QR"
            TYPE_RECEIVE_QR -> "$TYPE_RECEIVE_QR-${user.userId}"
            else -> ""
        }
    }

    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val view = View.inflate(ContextThemeWrapper(requireContext(), R.style.Custom), R.layout.view_qr_bottom, null)
        val viewBinding = ViewQrBottomBinding.bind(view)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        viewBinding.save.setOnClickListener {
            RxPermissions(requireActivity())
                .request(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                if (!isAdded) return@launch
                                val path = binding.bottomLl.capture(requireContext()) ?: return@launch
                                withContext(Dispatchers.Main) {
                                    context?.toast(getString(R.string.save_to, path))
                                }
                            }
                        } else {
                            requireContext().openPermissionSetting()
                        }
                    },
                    {
                        context?.toast(R.string.save_failure)
                    }
                )
            bottomSheet.dismiss()
        }
        viewBinding.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }
}
