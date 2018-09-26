package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_giphy_search_bottom_sheet.view.*
import kotlinx.android.synthetic.main.item_sticker.view.*
import one.mixin.android.R
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.loadGif
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.conversation.StickerFragment.Companion.COLUMN
import one.mixin.android.ui.conversation.StickerFragment.Companion.PADDING
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.vo.giphy.Gif
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.dip
import org.jetbrains.anko.support.v4.ctx
import org.jetbrains.anko.support.v4.toast
import retrofit2.HttpException
import timber.log.Timber

class GiphyBottomSheetFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "GiphyBottomSheetFragment"
        const val LIMIT = 50
        const val INTERVAL = 3000

        fun newInstance() = GiphyBottomSheetFragment()
    }

    private val adapter: GiphyAdapter by lazy {
        GiphyAdapter(
            (requireContext().displaySize().x - (COLUMN + 1) * padding) / COLUMN,
            object : GifListener {
                override fun onGifClick(url: String) {
                    callback?.onGiphyClick(url)
                    dismiss()
                }
            })
    }
    private var offset = 0
    private var searching = false
    private var lastSearchTime = 0L
    private val totalGifs = mutableListOf<Gif>()

    private val padding: Int by lazy {
        requireContext().dip(PADDING)
    }

    var callback: GiphyBottomSheetFragment.Callback? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_giphy_search_bottom_sheet, null)
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
            val h = requireContext().displaySize().y - requireContext().statusBarHeight() - requireContext().dip(56)
            setCustomViewHeight(h)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.sticker_rv.layoutManager = GridLayoutManager(context, COLUMN)
        contentView.sticker_rv.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, true))
        contentView.sticker_rv.adapter = adapter
        contentView.sticker_rv.addOnScrollListener(onScrollListener)
        contentView.search_et.setOnEditorActionListener(onEditorActionListener)
        contentView.cancel_tv.setOnClickListener { dismiss() }
        performSearch(false)
    }

    private fun update(list: List<Gif>) {
        offset += LIMIT
        totalGifs.addAll(list)
        adapter.submitList(totalGifs)
    }

    private fun performSearch(search: Boolean) {
        searching = true
        if (offset == 0) {
            contentView.pb.visibility = VISIBLE
        }
        val query = contentView.search_et.text.toString()
        if (search && query.isNotEmpty()) {
            bottomViewModel.searchGifs(query, LIMIT, offset)
        } else {
            bottomViewModel.trendingGifs(LIMIT, offset)
        }.autoDisposable(scopeProvider)
            .subscribe({ list ->
                if (!isAdded) return@subscribe
                if (search && offset == 0) {
                    adapter.notifyDataSetChanged()
                }
                update(list)
                searching = false
                contentView.pb?.visibility = GONE
            }, { t ->
                searching = false
                contentView.pb?.visibility = GONE
                Timber.d("Search gifs failed, t: ${t.printStackTrace()}")
                if (t is HttpException && t.code() == 429) {
                    toast("Giphy API rate limit exceeded")
                }
            })
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (!searching && !contentView.sticker_rv.canScrollVertically(1)) {
                val cur = System.currentTimeMillis()
                if (cur - lastSearchTime < INTERVAL) return

                lastSearchTime = cur
                performSearch(false)
            }
        }
    }

    private val onEditorActionListener = TextView.OnEditorActionListener { _, actionId, _ ->
        ctx
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            if (!searching) {
                offset = 0
                totalGifs.clear()
                performSearch(true)
                contentView.search_et.hideKeyboard()
            }
            return@OnEditorActionListener true
        }
        false
    }

    interface Callback {
        fun onGiphyClick(url: String)
    }

    class GiphyAdapter(private val size: Int, private val listener: GifListener) : ListAdapter<Gif, ItemHolder>(Gif.DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_sticker, parent, false))

        override fun onBindViewHolder(holder: ItemHolder, pos: Int) {
            val params = holder.itemView.layoutParams
            params.width = size
            params.height = size
            holder.itemView.layoutParams = params
            val item = (holder.itemView as ViewGroup).getChildAt(0) as ImageView
            val image = getItem(pos).images.fixed_width
            item.loadGif(image.url)
            item.updateLayoutParams<ViewGroup.LayoutParams> {
                width = size
                height = size
            }
            holder.itemView.sticker_iv.loadGif(image.url)
            holder.itemView.setOnClickListener { listener.onGifClick(image.url) }
        }
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface GifListener {
        fun onGifClick(url: String)
    }
}