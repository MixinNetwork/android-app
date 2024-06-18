package one.mixin.android.ui.home.inscription

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentCollectiblesBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.home.inscription.menu.SortMenuAdapter
import one.mixin.android.ui.home.inscription.menu.SortMenuData
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

    private val collectionAdapter by lazy {
        CollectionAdapter {
            // Todo
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
            radioCollectibles.isChecked = true
            collectiblesRv.addItemDecoration(StickerSpacingItemDecoration(2, padding, true))

            collectiblesRv.layoutManager = GridLayoutManager(requireContext(), 2)
            collectiblesRv.adapter = collectiblesAdapter

            web3ViewModel.collectibles().observe(this@CollectiblesFragment.viewLifecycleOwner) {
                binding.collectiblesVa.displayedChild =
                    if (it.isEmpty()) {
                        1
                    } else {
                        0
                    }
                collectiblesAdapter.list = it
            }
            dropSort.setOnClickListener {
                binding.sortArrow.animate().rotation(-180f).setDuration(200).start()
                sortMenu.show()
            }

            radioGroupCollectibles.setOnCheckedChangeListener { _, id ->
                when (id) {
                    R.id.radio_collectibles -> {
                        collectiblesRv.adapter = collectiblesAdapter
                        web3ViewModel.collectibles().observe(this@CollectiblesFragment.viewLifecycleOwner) {
                            binding.collectiblesVa.displayedChild =
                                if (it.isEmpty()) {
                                    1
                                } else {
                                    0
                                }
                            collectiblesAdapter.list = it
                        }
                    }

                    R.id.radio_collection -> {
                        collectiblesRv.adapter = collectionAdapter
                        web3ViewModel.collections().observe(this@CollectiblesFragment.viewLifecycleOwner) {
                            binding.collectiblesVa.displayedChild =
                                if (it.isEmpty()) {
                                    1
                                } else {
                                    0
                                }
                            collectionAdapter.list = it
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private val sortMenu by lazy {
        val menuItems = listOf(
            SortMenuData(1, R.drawable.ic_recent, R.string.Recent),
            SortMenuData(2, R.drawable.ic_alphabetical,  R.string.Alphabetical),
        )
        ListPopupWindow(requireContext()).apply {
            anchorView = binding.dropSort
            setAdapter(SortMenuAdapter(requireContext(), menuItems))
            setOnItemClickListener { _, _, position, _ ->
                val selectedItem = menuItems[position]
                binding.dropTv.setText(selectedItem.title)
                // Todo
                dismiss()
            }
            width = requireContext().dpToPx(250f)
            height = ListPopupWindow.WRAP_CONTENT
            isModal = true
            setBackgroundDrawable(ColorDrawable(requireContext().colorAttr(R.attr.bg_white)))
            setDropDownGravity(Gravity.END)
            horizontalOffset = requireContext().dpToPx(2f)
            verticalOffset = requireContext().dpToPx(10f)
            setOnDismissListener {
                binding.sortArrow.animate().rotation(0f).setDuration(200).start()
            }
        }
    }
}
