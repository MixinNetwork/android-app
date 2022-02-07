package one.mixin.android.ui.sticker

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentStickerStoreBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.StickerAlbumAdded
import one.mixin.android.widget.viewpager2.ScaleTransformer

@AndroidEntryPoint
class StickerStoreFragment : BaseFragment(R.layout.fragment_sticker_store) {
    companion object {
        const val TAG = "StickerStoreFragment"
        fun newInstance() = StickerStoreFragment()
    }

    private val viewModel by viewModels<ConversationViewModel>()
    private val binding by viewBinding(FragmentStickerStoreBinding::bind)

    private val bannerAdapter = BannerAdapter()
    private val albumAdapter: AlbumAdapter by lazy {
        AlbumAdapter(parentFragmentManager, viewModel, viewLifecycleOwner) { albumId ->
            lifecycleScope.launch {
                val maxOrder = viewModel.findMaxOrder()
                viewModel.updateAlbumAdded(StickerAlbumAdded(albumId, true, maxOrder + 1))
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.apply {
                leftIb.setOnClickListener {
                    activity?.onBackPressed()
                }
                rightAnimator.setOnClickListener {
                    navTo(StickerAlbumManagementFragment.newInstance(), StickerAlbumManagementFragment.TAG)
                }
            }
            bannerPager.apply {
                @Suppress("UNCHECKED_CAST")
                setAdapter(bannerAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>)
                setPageMargin(30.dp, 10.dp)
                    .addPageTransformer(ScaleTransformer())
            }
            bannerAdapter.bannerListener = object : BannerListener {
                override fun onBannerClick(banner: Banner) {
                    StickerAlbumBottomSheetFragment.newInstance(banner.albumId)
                        .showNow(parentFragmentManager, StickerAlbumBottomSheetFragment.TAG)
                }
            }
            albumRv.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext())
                adapter = albumAdapter
            }
            viewModel.observeSystemAlbums().observe(viewLifecycleOwner) { albums ->
                val banners = albums
                    .filter { !it.banner.isNullOrEmpty() }
                    .take(3)
                    .map { Banner(it.albumId, requireNotNull(it.banner)) }
                bannerAdapter.data = banners

                albumAdapter.submitList(albums)
            }
        }
        viewModel.refreshStickerAlbums()
    }
}
