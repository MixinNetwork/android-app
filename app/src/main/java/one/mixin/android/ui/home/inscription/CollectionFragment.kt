package one.mixin.android.ui.home.inscription

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentCollectionBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.ui.home.inscription.CollectionActivity.Companion.ARGS_COLLECTION
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.vo.safe.SafeCollection

@AndroidEntryPoint
class CollectionFragment : BaseFragment() {
    companion object {
        const val TAG = "CollectionFragment"

        fun newInstance(collection: SafeCollection) = CollectionFragment().apply {
            withArgs {
                putParcelable(ARGS_COLLECTION, collection)
            }
        }
    }

    private val safeCollection by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_COLLECTION, SafeCollection::class.java))
    }

    private val padding: Int by lazy {
        15.dp
    }

    private var _binding: FragmentCollectionBinding? = null

    private val binding get() = requireNotNull(_binding)

    private val web3ViewModel by viewModels<Web3ViewModel>()

    private val collectiblesAdapter by lazy {
        CollectiblesHeaderAdapter(safeCollection) {
            InscriptionActivity.show(requireContext(), it.inscriptionHash)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
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
            collectiblesRv.addItemDecoration(StickerSpacingItemDecoration(2, padding, true, skip = true))

            val lm = GridLayoutManager(requireContext(), 2)
            lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (position == 0) 2 else 1
                }
            }
            collectiblesRv.layoutManager = lm
            collectiblesRv.adapter = collectiblesAdapter
            web3ViewModel.collectibles(SortOrder.Recent).observe(this@CollectionFragment.viewLifecycleOwner) {
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
}
