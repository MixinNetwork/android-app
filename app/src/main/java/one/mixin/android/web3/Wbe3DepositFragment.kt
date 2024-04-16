
package one.mixin.android.web3

import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWeb3DepositBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment

@AndroidEntryPoint
class Wbe3DepositFragment : BaseFragment() {
    companion object {
        const val TAG = "Wbe3DepositFragment"
    }

    private var _binding: FragmentWeb3DepositBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWeb3DepositBinding.inflate(inflater, container, false).apply { this.root.setOnClickListener { } }
        binding.root.setOnClickListener {  }
        binding.title.setOnClickListener {  }
        binding.title.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        lifecycleScope.launch {
            val address = PropertyHelper.findValueByKey(Constants.Account.ChainAddress.EVM_ADDRESS, "")
            binding.copy.setOnClickListener {
                context?.heavyClickVibrate()
                context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, address))
                toast(R.string.copied_to_clipboard)
            }
            binding.address.text = address
            val qr = this@Wbe3DepositFragment.binding.qr
            val qrAvatar = this@Wbe3DepositFragment.binding.qrAvatar
            qrAvatar.bg.setImageResource(R.drawable.ic_web3_logo_mixin)
            qr.post {
                Observable.create<Pair<Bitmap, Int>> { e ->
                    val r = address.generateQRCode(qr.width)
                    e.onNext(r)
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(scopeProvider)
                    .subscribe(
                        { r ->
                            qrAvatar.layoutParams =
                                qrAvatar.layoutParams.apply {
                                    width = r.second
                                    height = r.second
                                }
                            qr.setImageBitmap(r.first)
                        },
                        {
                        },
                    )
            }
        }
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}