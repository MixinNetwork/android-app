package one.mixin.android.ui.home.inscription

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentCollectiblesBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.dp
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.search.SearchInscriptionFragment
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.util.rxpermission.RxPermissions
import javax.inject.Inject

@AndroidEntryPoint
class CollectiblesFragment : BaseFragment() {
    companion object {
        const val TAG = "CollectiblesFragment"

        fun newInstance() = CollectiblesFragment()
    }

    private val padding: Int by lazy {
        15.dp
    }

    @Inject
    lateinit var tipCounterSynced: TipCounterSyncedLiveData
    private var _binding: FragmentCollectiblesBinding? = null

    private val binding get() = requireNotNull(_binding)

    private val web3ViewModel by viewModels<Web3ViewModel>()

    private val collectiblesAdapter by lazy {
        CollectiblesAdapter {
            InscriptionActivity.show(requireContext(), it.inscriptionHash)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCollectiblesBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            root.setOnClickListener {
                // do nothing
            }

            searchIb.setOnClickListener {
                activity?.addFragment(
                    this@CollectiblesFragment,
                    SearchInscriptionFragment(),
                    SearchInscriptionFragment.TAG,
                )
            }

            scanIb.setOnClickListener {
                RxPermissions(requireActivity()).request(Manifest.permission.CAMERA).autoDispose(stopScope).subscribe { granted ->
                    if (granted) {
                        (requireActivity() as? MainActivity)?.showCapture(true)
                    } else {
                        context?.openPermissionSetting()
                    }
                }
            }

            settingIb.setOnClickListener {
                SettingActivity.show(requireContext(), compose = false)
            }
            collectiblesRv.addItemDecoration(StickerSpacingItemDecoration(2, padding, true))

            collectiblesRv.layoutManager = GridLayoutManager(requireContext(), 2)
            collectiblesRv.adapter = collectiblesAdapter
        }
        web3ViewModel.inscriptions().observe(this.viewLifecycleOwner) {
            binding.collectiblesVa.displayedChild =
                if (it.isEmpty()) {
                    1
                } else {
                    0
                }
            collectiblesAdapter.list = it
        }
    }
}
