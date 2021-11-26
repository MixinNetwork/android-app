package one.mixin.android.ui.sticker

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentStickerStoreBinding
import one.mixin.android.extension.dp
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.util.viewBinding
import kotlin.math.abs
import kotlin.math.max

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
                adapter = bannerAdapter
                offscreenPageLimit = 3
                currentItem = 1
                val pageMargin = 20.dp
                val pageOffset = 10.dp
                setPageTransformer { page, position ->
                    val offset = position * -(2 * pageOffset + pageMargin)
                    if (position < -1) {
                        page.translationX = -offset
                    } else if (position <= 1) {
                        val scale = max(0.7f, abs(position - 0.14285715f))
                        page.translationX = offset
                        page.scaleY = scale
                    } else {
                        page.translationX = offset
                    }
                }
            }
            viewModel.getSystemAlbums().observe(viewLifecycleOwner) { albums ->
                val banners = albums
                    .filter { !it.banner.isNullOrEmpty() }
                    .take(3)
                    .map { Banner(requireNotNull(it.banner)) }
                bannerAdapter.submitList(banners)
            }
            albumRv.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext())
                adapter = albumAdapter
            }
        }
    }
}
