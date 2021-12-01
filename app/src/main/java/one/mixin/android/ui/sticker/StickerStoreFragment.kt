package one.mixin.android.ui.sticker

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentStickerStoreBinding
import one.mixin.android.extension.dp
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.util.viewBinding
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
    private val albumAdapter = AlbumAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.apply {
                leftIb.setOnClickListener {
                    activity?.onBackPressed()
                }
                rightAnimator.setOnClickListener {
                }
            }
            bannerPager.apply {
                @Suppress("UNCHECKED_CAST")
                setAdapter(bannerAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>)
                setPageMargin(20.dp, 4.dp)
                    .addPageTransformer(ScaleTransformer())
            }
            viewModel.getSystemAlbums().observe(viewLifecycleOwner) { albums ->
                val banners = albums
                    .filter { !it.banner.isNullOrEmpty() }
                    .take(3)
                    .map { Banner(requireNotNull(it.banner)) }
                bannerAdapter.data = banners
            }
            albumRv.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext())
                adapter = albumAdapter
            }
        }
    }
}
