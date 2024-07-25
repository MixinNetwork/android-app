package one.mixin.android.ui.home.inscription

import android.Manifest
import android.annotation.SuppressLint
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
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.FragmentCollectiblesBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putInt
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.ui.common.BaseFragment
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
        private const val TYPE_COLLECTION = 0
        private const val TYPE_COLLECTIBLES= 1
        fun newInstance() = CollectiblesFragment()
    }

    private val padding: Int by lazy {
        16.dp
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
            CollectionActivity.show(requireContext(), it)
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
            if (type == TYPE_COLLECTIBLES) {
                radioCollectibles.isChecked = true
            } else {
                radioCollection.isChecked = true
            }
            collectiblesRv.addItemDecoration(InscriptionSpacingItemDecoration(2, padding))

            collectiblesRv.layoutManager = GridLayoutManager(requireContext(), 2)
            collectiblesRv.adapter = collectiblesAdapter

            dropSort.setOnClickListener {
                binding.sortArrow.animate().rotation(-180f).setDuration(200).start()
                menuAdapter.checkPosition = if (sortOrder == SortOrder.Alphabetical) 1 else 0
                menuAdapter.notifyDataSetChanged()
                onMenuShow()
                sortMenu.show()
            }

            radioGroupCollectibles.setOnCheckedChangeListener { _, id ->
                type = if (R.id.radio_collectibles == id) {
                    TYPE_COLLECTIBLES
                } else {
                    TYPE_COLLECTION
                }
            }
        }
        bindData()
    }

    private val onMenuDismiss = {
        binding.dropSort.setBackgroundResource(R.drawable.bg_inscription_radio)
        binding.dropTv.setTextColor(requireContext().colorAttr(R.attr.text_primary))
    }

    private val onMenuShow = {
        binding.dropSort.setBackgroundResource(R.drawable.bg_inscription_drop)
        binding.dropTv.setTextColor(0xFF4B7CDD.toInt())
    }

    private fun bindData() {
        binding.dropTv.setText(if (sortOrder == SortOrder.Recent) R.string.Recent else R.string.Alphabetical)
        when (type) {
            TYPE_COLLECTIBLES -> {
                binding.collectiblesRv.adapter = collectiblesAdapter
                web3ViewModel.collectibles(sortOrder).observe(this@CollectiblesFragment.viewLifecycleOwner) {
                    binding.collectiblesVa.displayedChild =
                        if (it.isEmpty()) {
                            1
                        } else {
                            0
                        }
                    collectiblesAdapter.list = it
                }
            }

            else -> {
                binding.collectiblesRv.adapter = collectionAdapter
                web3ViewModel.collections(sortOrder).observe(this@CollectiblesFragment.viewLifecycleOwner) {
                    binding.collectiblesVa.displayedChild =
                        if (it.isEmpty()) {
                            1
                        } else {
                            0
                        }
                    collectionAdapter.list = it
                }
            }
        }
    }

    private var sortOrder = SortOrder.fromInt(MixinApplication.appContext.defaultSharedPreferences.getInt(Constants.Account.PREF_INSCRIPTION_ORDER, SortOrder.Alphabetical.value))
        set(value) {
            if (field != value) {
                field = value
                defaultSharedPreferences.putInt(Constants.Account.PREF_INSCRIPTION_ORDER, value.value)
                bindData()
            }
        }

    private var type = MixinApplication.appContext.defaultSharedPreferences.getInt(Constants.Account.PREF_INSCRIPTION_TYPE, TYPE_COLLECTIBLES)
        set(value) {
            if (field != value) {
                field = value
                defaultSharedPreferences.putInt(Constants.Account.PREF_INSCRIPTION_TYPE, value)
                bindData()
            }
        }

    private val sortMenu by lazy {
        ListPopupWindow(requireContext()).apply {
            anchorView = binding.dropSort
            setAdapter(menuAdapter)
            setOnItemClickListener { _, _, position, _ ->
                sortOrder = if (position == 0) {
                    SortOrder.Recent
                } else {
                    SortOrder.Alphabetical
                }
                dismiss()
            }
            width = requireContext().dpToPx(250f)
            height = ListPopupWindow.WRAP_CONTENT
            isModal = true
            setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_round_white_8dp))
            setDropDownGravity(Gravity.END)
            horizontalOffset = requireContext().dpToPx(2f)
            verticalOffset = requireContext().dpToPx(10f)
            setOnDismissListener {
                onMenuDismiss()
                binding.sortArrow.animate().rotation(0f).setDuration(200).start()
            }
        }
    }

    private val menuAdapter: SortMenuAdapter by lazy {
        val menuItems = listOf(
            SortMenuData(SortOrder.Recent, R.drawable.ic_recent, R.string.Recent),
            SortMenuData(SortOrder.Alphabetical, R.drawable.ic_alphabetical, R.string.Alphabetical),
        )
        SortMenuAdapter(requireContext(), menuItems)
    }
}
