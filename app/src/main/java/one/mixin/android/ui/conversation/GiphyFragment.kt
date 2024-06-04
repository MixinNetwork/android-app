package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentStickerBinding
import one.mixin.android.extension.clear
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadGif
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.realSize
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.recyclerview.FooterAdapter
import one.mixin.android.ui.conversation.StickerFragment.Companion.COLUMN
import one.mixin.android.ui.conversation.adapter.StickerAlbumAdapter
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.giphy.Gif
import one.mixin.android.vo.giphy.Image
import one.mixin.android.widget.DraggableRecyclerView
import retrofit2.HttpException
import timber.log.Timber

@AndroidEntryPoint
class GiphyFragment : BaseFragment(R.layout.fragment_sticker) {
    companion object {
        const val TAG = "GiphyFragment"

        fun newInstance() = GiphyFragment()
    }

    private val padding: Int by lazy {
        StickerFragment.PADDING.dp
    }
    private val giphyAdapter: GiphyAdapter by lazy { GiphyAdapter() }

    private val stickerViewModel by viewModels<ConversationViewModel>()

    var callback: StickerAlbumAdapter.Callback? = null
    var rvCallback: DraggableRecyclerView.Callback? = null

    private val binding by viewBinding(FragmentStickerBinding::bind)

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            stickerRv.layoutManager =
                GridLayoutManager(context, COLUMN).apply {
                    spanSizeLookup =
                        object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(pos: Int): Int {
                                return if (pos == giphyAdapter.itemCount - 1) {
                                    COLUMN
                                } else {
                                    1
                                }
                            }
                        }
                }
            val foot = layoutInflater.inflate(R.layout.view_giphy_foot, stickerRv, false)
            giphyAdapter.footerView = foot
            stickerRv.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, true))
            giphyAdapter.size = (requireContext().realSize().x - (COLUMN + 1) * padding) / COLUMN
            stickerRv.adapter = giphyAdapter
            giphyAdapter.setOnGiphyListener(
                object : GiphyListener {
                    override fun onItemClick(
                        pos: Int,
                        image: Image,
                        previewUrl: String,
                    ) {
                        callback?.onGiphyClick(image, previewUrl)
                    }

                    override fun onSearchClick() {
                        val f = GiphyBottomSheetFragment.newInstance()
                        f.showNow(parentFragmentManager, GiphyBottomSheetFragment.TAG)
                        f.callback =
                            object : GiphyBottomSheetFragment.Callback {
                                override fun onGiphyClick(
                                    image: Image,
                                    previewUrl: String,
                                ) {
                                    callback?.onGiphyClick(image, previewUrl)
                                }
                            }
                    }
                },
            )
            stickerRv.callback =
                object : DraggableRecyclerView.Callback {
                    override fun onScroll(dis: Float) {
                        rvCallback?.onScroll(dis)
                    }

                    override fun onRelease(fling: Int) {
                        rvCallback?.onRelease(fling)
                    }
                }
            stickerProgress.visibility = View.VISIBLE
            stickerViewModel.trendingGifs(26, 0)
                .autoDispose(stopScope)
                .subscribe(
                    { list ->
                        if (viewDestroyed()) return@subscribe
                        giphyAdapter.data = list
                        giphyAdapter.notifyDataSetChanged()
                        stickerProgress.visibility = View.GONE
                    },
                    { t ->
                        Timber.d("Trending gifs failed, t: ${t.printStackTrace()}")
                        if (t is HttpException && t.code() == 429) {
                            toast("Giphy API rate limit exceeded")
                        }
                        stickerProgress.visibility = View.GONE
                    },
                )
        }
    }

    private class GiphyAdapter : FooterAdapter<Gif>() {
        private var listener: GiphyListener? = null
        var size: Int = 0

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            if (position == itemCount - 1) {
                return
            }

            val params = holder.itemView.layoutParams
            params.width = size
            params.height = (size * (3f / 4)).toInt()
            holder.itemView.layoutParams = params
            val ctx = holder.itemView.context
            val item = (holder.itemView as ViewGroup).getChildAt(0) as ImageView
            if (position == 0) {
                item.updateLayoutParams<FrameLayout.LayoutParams> {
                    width = size - 20.dp
                    height = (width * (3f / 4)).toInt()
                }
                item.clear()
                item.setImageDrawable(AppCompatResources.getDrawable(ctx, R.drawable.ic_gif_search))
                item.setOnClickListener { listener?.onSearchClick() }
            } else {
                val images = data!![position - 1].images
                val previewImage = images.fixed_width_downsampled
                val sendImage = images.fixed_width
                item.loadImage(previewImage.url, R.drawable.ic_giphy_place_holder)
                item.setOnClickListener { listener?.onItemClick(position, sendImage, previewImage.url) }
                item.updateLayoutParams<ViewGroup.LayoutParams> {
                    width = size
                    height = (size * (3f / 4)).toInt()
                }
            }
        }

        override fun getNormalViewHolder(
            context: Context,
            parent: ViewGroup,
        ): NormalHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sticker, parent, false)
            return NormalHolder(view)
        }

        override fun getItemCount(): Int =
            data.notNullWithElse(
                {
                    if (footerView != null) it.size + 2 else it.size + 1
                },
                0,
            )

        fun setOnGiphyListener(giphyListener: GiphyListener) {
            listener = giphyListener
        }
    }

    interface GiphyListener {
        fun onItemClick(
            pos: Int,
            image: Image,
            previewUrl: String,
        )

        fun onSearchClick()
    }
}
