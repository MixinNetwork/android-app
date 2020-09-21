package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_giphy_search_bottom_sheet.view.*
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.loadGif
import one.mixin.android.extension.realSize
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.recyclerview.FooterListAdapter
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.conversation.StickerFragment.Companion.COLUMN
import one.mixin.android.ui.conversation.StickerFragment.Companion.PADDING
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.vo.giphy.Gif
import one.mixin.android.vo.giphy.Image
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.dip
import retrofit2.HttpException
import timber.log.Timber

@AndroidEntryPoint
class GiphyBottomSheetFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "GiphyBottomSheetFragment"
        const val LIMIT = 51
        const val INTERVAL = 4000

        const val POS_RV = 0
        const val POS_PB = 1
        const val POS_EMPTY = 2

        fun newInstance() = GiphyBottomSheetFragment()

        @JvmField
        var shown = false
    }

    private val adapter: GiphyAdapter by lazy {
        GiphyAdapter(
            (requireContext().realSize().x - (COLUMN + 1) * padding) / COLUMN,
            object : GifListener {
                override fun onGifClick(image: Image, previewUrl: String) {
                    callback?.onGiphyClick(image, previewUrl)
                    dismiss()
                }
            }
        )
    }
    private var offset = 0
    private var fetching = false
    private var searching = false
    private var noMore = false
    private var lastSearchTime = 0L
    private val totalGifs = mutableListOf<Gif>()

    private val padding: Int by lazy {
        requireContext().dip(PADDING)
    }

    var callback: Callback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        shown = true
    }

    override fun onDetach() {
        shown = false
        contentView.hideKeyboard()
        super.onDetach()
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_giphy_search_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)

        contentView.sticker_rv.layoutManager = GridLayoutManager(context, COLUMN).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(pos: Int): Int {
                    return if (pos == adapter.itemCount - 1) {
                        COLUMN
                    } else {
                        1
                    }
                }
            }
        }
        val foot = layoutInflater.inflate(R.layout.view_giphy_foot, contentView.sticker_rv, false)
        adapter.footerView = foot
        contentView.sticker_rv.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, true))
        contentView.sticker_rv.adapter = adapter
        contentView.sticker_rv.addOnScrollListener(onScrollListener)
        contentView.search_et.setOnEditorActionListener(onEditorActionListener)
        contentView.cancel_tv.setOnClickListener {
            contentView.hideKeyboard()
            dismiss()
        }
        performSearch()
    }

    private fun update(list: List<Gif>) {
        offset += LIMIT
        totalGifs.addAll(list)
        adapter.submitList(totalGifs)
    }

    private fun performSearch() {
        fetching = true
        if (offset == 0) {
            contentView.sticker_va.displayedChild = POS_PB
        }
        if (searching) {
            val query = contentView.search_et.text.toString()
            bottomViewModel.searchGifs(query, LIMIT, offset)
        } else {
            bottomViewModel.trendingGifs(LIMIT, offset)
        }.autoDispose(stopScope)
            .subscribe(
                { list ->
                    if (!isAdded) return@subscribe
                    if (offset == 0) {
                        adapter.notifyDataSetChanged()
                    }
                    if (list.isEmpty()) {
                        contentView.sticker_va.displayedChild = POS_EMPTY
                    } else {
                        contentView.sticker_va.displayedChild = POS_RV
                        update(list)
                    }
                    if (list.size < LIMIT) {
                        noMore = true
                    }
                    fetching = false
                },
                { t ->
                    fetching = false
                    contentView.sticker_va.displayedChild = POS_EMPTY
                    Timber.d("Search gifs failed, t: ${t.printStackTrace()}")
                    if (t is HttpException && t.code() == 429) {
                        toast("Giphy API rate limit exceeded")
                    }
                }
            )
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (!fetching && !contentView.sticker_rv.canScrollVertically(1)) {
                val cur = System.currentTimeMillis()
                if (noMore || cur - lastSearchTime < INTERVAL) return

                lastSearchTime = cur
                performSearch()
            }
        }
    }

    private val onEditorActionListener = TextView.OnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            if (!fetching) {
                offset = 0
                searching = contentView.search_et.text.toString().trim().isNotEmpty()
                noMore = false
                totalGifs.clear()
                contentView.sticker_rv.scrollToPosition(0)
                performSearch()
                contentView.search_et.hideKeyboard()
            }
            return@OnEditorActionListener true
        }
        false
    }

    interface Callback {
        fun onGiphyClick(image: Image, previewUrl: String)
    }

    class GiphyAdapter(private val size: Int, private val listener: GifListener) : FooterListAdapter<Gif, RecyclerView.ViewHolder>(Gif.DIFF_CALLBACK) {
        override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder {
            return NormalHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_sticker, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            if (pos == itemCount - 1) return

            val params = holder.itemView.layoutParams
            params.width = size
            params.height = (size * (3f / 4)).toInt()
            holder.itemView.layoutParams = params
            val item = (holder.itemView as ViewGroup).getChildAt(0) as ImageView
            val images = getItem(pos).images
            val previewImage = images.fixed_width_downsampled
            val sendImage = images.fixed_width
            item.updateLayoutParams<ViewGroup.LayoutParams> {
                width = size
                height = (size * (3f / 4)).toInt()
            }
            item.loadGif(previewImage.url, centerCrop = true, holder = R.drawable.ic_giphy_place_holder)
            holder.itemView.setOnClickListener { listener.onGifClick(sendImage, previewImage.url) }
        }
    }

    interface GifListener {
        fun onGifClick(image: Image, previewUrl: String)
    }
}
