package one.mixin.android.ui.group

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
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
import one.mixin.android.R
import one.mixin.android.databinding.FragmentInviteQrBottomBinding
import one.mixin.android.extension.capture
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class InviteQrBottomFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "InviteQrBottomFragment"
        private const val ICON_URL = "icon_url"
        private const val NAME = "name"
        private const val URL = "url"

        fun newInstance(name: String?, iconUrl: String?, url: String?) = InviteQrBottomFragment().apply {
            arguments = bundleOf(
                ICON_URL to iconUrl,
                NAME to name,
                URL to url
            )
        }
    }

    private val binding by viewBinding(FragmentInviteQrBottomBinding::inflate)

    private val iconUrl: String? by lazy {
        requireArguments().getString(ICON_URL)
    }

    private val name: String? by lazy {
        requireArguments().getString(NAME)
    }

    private val url: String? by lazy {
        requireArguments().getString(URL)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            title.rightIv.setOnClickListener { dismiss() }
            title.setSubTitle(name ?: "")
            avatar.setGroup(iconUrl)
            saveIv.setOnClickListener {
                RxPermissions(requireActivity())
                    .request(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe(
                        { granted ->
                            if (granted) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    if (!isAdded) return@launch
                                    val path = contentLl.capture(requireContext())
                                    withContext(Dispatchers.Main) {
                                        toast(getString(R.string.save_to, path))
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
            }

            qr.post {
                Observable.create<Pair<Bitmap, Int>?> { e ->
                    url?.generateQRCode(qr.width)?.let {
                        e.onNext(it)
                    }
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(stopScope)
                    .subscribe(
                        { r ->
                            avatar.layoutParams = avatar.layoutParams.apply {
                                width = r.second
                                height = r.second
                            }
                            qr.setImageBitmap(r.first)
                        },
                        {
                        }
                    )
            }
        }
    }
}
