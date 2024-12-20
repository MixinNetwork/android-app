
package one.mixin.android.web3.receive

import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWeb3AddressBinding
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.exploreSolana

@AndroidEntryPoint
class Web3AddressFragment : BaseFragment() {
    companion object {
        const val TAG = "Wbe3ReceiveFragment"
    }

    private var _binding: FragmentWeb3AddressBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWeb3AddressBinding.inflate(inflater, container, false).apply { this.root.setOnClickListener { } }
        binding.root.setOnClickListener { }
        binding.title.setOnClickListener { }
        binding.title.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        lifecycleScope.launch {
            val address = getExploreAddress(requireContext())
            binding.copy.setOnClickListener {
                context?.heavyClickVibrate()
                context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, address))
                toast(R.string.copied_to_clipboard)
            }
            binding.address.text = address
            val qr = this@Web3AddressFragment.binding.qr
            val qrAvatar = this@Web3AddressFragment.binding.qrAvatar
            val isSolana = exploreSolana(requireContext())
            if (isSolana) {
                qrAvatar.bg.setImageResource(R.drawable.ic_web3_logo_sol)
                binding.avatar1.setImageResource(R.drawable.ic_web3_chain_sol)
                binding.avatar2.isVisible = false
                binding.avatar3.isVisible = false
                binding.avatar4.isVisible = false
                binding.avatar5.isVisible = false
                binding.avatar6.isVisible = false
                binding.avatar7.isVisible = false
                binding.bottomHintTv.setText(R.string.web3_deposit_description_solana)
            } else {
                qrAvatar.bg.setImageResource(R.drawable.ic_web3_logo_eth)
                binding.avatar1.setImageResource(R.drawable.ic_web3_chain_eth)
                binding.avatar2.isVisible = true
                binding.avatar3.isVisible = true
                binding.avatar4.isVisible = true
                binding.avatar5.isVisible = true
                binding.avatar6.isVisible = true
                binding.avatar7.isVisible = true
                binding.bottomHintTv.setText(R.string.web3_deposit_description_evm)
            }
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
