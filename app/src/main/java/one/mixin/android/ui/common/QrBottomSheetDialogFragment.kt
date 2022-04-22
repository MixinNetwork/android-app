package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.net.toUri
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
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.Constants.Scheme.TRANSFER
import one.mixin.android.R
import one.mixin.android.databinding.FragmentQrBottomSheetBinding
import one.mixin.android.databinding.ViewQrBottomBinding
import one.mixin.android.extension.capture
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.shareMedia
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BadgeCircleImageView.Companion.END_BOTTOM
import one.mixin.android.widget.BottomSheet
import java.io.File

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

    private val binding by viewBinding(FragmentQrBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.title.centerTitle()
        binding.title.leftIv.setOnClickListener { dismiss() }
        binding.title.rightIv.setOnClickListener { showBottom() }
        if (type == TYPE_MY_QR) {
            binding.title.titleTv.text = getString(R.string.My_QR_Code)
            binding.tipTv.text = getString(R.string.scan_code_add_me)
        } else if (type == TYPE_RECEIVE_QR) {
            binding.title.titleTv.text = getString(R.string.Receive_Money)
            binding.tipTv.text = getString(R.string.contact_receive_tip)
        }
        binding.shareBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                if (!isAdded) return@launch
                val path = binding.bottomLl.capture(requireContext()) ?: return@launch
                withContext(Dispatchers.Main) {
                    requireContext().shareMedia(false, File(path).toUri().toString())
                }
            }
        }
        bottomViewModel.findUserById(userId).observe(
            this
        ) { user ->
            if (user == null) {
                bottomViewModel.refreshUser(userId, true)
            } else {
                binding.badgeView.bg.setInfo(user.fullName, user.avatarUrl, user.userId)
                binding.idTv.text = getString(R.string.contact_mixin_id, user.identityNumber)
                if (type == TYPE_RECEIVE_QR) {
                    binding.badgeView.badge.setImageResource(R.drawable.ic_contacts_receive_blue)
                    binding.badgeView.pos = END_BOTTOM
                }
                binding.qr.post {
                    Observable.create<Pair<Bitmap, Int>> { e ->
                        val account = Session.getAccount() ?: return@create
                        val code = when (type) {
                            TYPE_MY_QR -> account.codeUrl
                            TYPE_RECEIVE_QR -> "$TRANSFER/${user.userId}"
                            else -> ""
                        }

                        val r = code.generateQRCode(binding.qr.width)
                        e.onNext(r)
                    }.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .autoDispose(stopScope)
                        .subscribe(
                            { r ->
                                binding.badgeView.layoutParams =
                                    binding.badgeView.layoutParams.apply {
                                        width = r.second
                                        height = r.second
                                    }
                                binding.qr.setImageBitmap(r.first)
                            },
                            {
                            }
                        )
                }
            }
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
                .request(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                if (!isAdded) return@launch
                                val path = binding.bottomLl.capture(requireContext())
                                withContext(Dispatchers.Main) {
                                    if (path.isNullOrBlank()) {
                                        toast(getString(R.string.Save_failure))
                                    } else {
                                        toast(getString(R.string.Save_to, path))
                                    }
                                }
                            }
                        } else {
                            requireContext().openPermissionSetting()
                        }
                    },
                    {
                        toast(R.string.Save_failure)
                    }
                )
            bottomSheet.dismiss()
        }
        viewBinding.cancel.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet.show()
    }
}
