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
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.ui.home.inscription.CollectionActivity.Companion.ARGS_HASH
import one.mixin.android.ui.home.web3.Web3ViewModel

@AndroidEntryPoint
class CollectionFragment : BaseFragment() {
    companion object {
        const val TAG = "CollectionFragment"

        fun newInstance(collectionHash: String) = CollectionFragment().apply {
            withArgs {
                putString(ARGS_HASH, collectionHash)
            }
        }
    }

    private val hash by lazy {
        requireNotNull(requireArguments().getString(ARGS_HASH))
    }

    private val padding: Int by lazy {
        15.dp
    }

    private var _binding: FragmentCollectionBinding? = null

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
            collectiblesRv.addItemDecoration(StickerSpacingItemDecoration(2, padding, true))

            collectiblesRv.layoutManager = GridLayoutManager(requireContext(), 2)
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
