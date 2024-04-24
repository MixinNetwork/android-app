
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
import one.mixin.android.databinding.FragmentWeb3DepositSelectBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.navTo
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment

@AndroidEntryPoint
class Wbe3DepositSelectFragment : BaseFragment() {
    companion object {
        const val TAG = "Wbe3DepositSelectFragment"
    }

    private var _binding: FragmentWeb3DepositSelectBinding? = null
    private val binding get() = requireNotNull(_binding)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWeb3DepositSelectBinding.inflate(inflater, container, false).apply { this.root.setOnClickListener { } }
        binding.root.setOnClickListener {  }
        binding.title.setOnClickListener {  }
        binding.title.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        binding.walletRl.setOnClickListener {
            // Todo
        }
        binding.addressRl.setOnClickListener {
            navTo(Wbe3DepositFragment(), Wbe3DepositFragment.TAG)
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