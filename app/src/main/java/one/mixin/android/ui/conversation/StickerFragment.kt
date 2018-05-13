package one.mixin.android.ui.conversation

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.fragment_sticker.*
import one.mixin.android.R
import one.mixin.android.extension.displaySize
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.vo.Sticker
import org.jetbrains.anko.dip
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import javax.inject.Inject

class StickerFragment : BaseFragment() {

    companion object {
        const val TAG = "StickerFragment"
        const val ARGS_ALBUM_ID = "args_album_id"
        const val ARGS_RECENT = "args_recent"
        const val PADDING = 10
        const val COLUMN = 3

        fun newInstance(id: String? = null, recent: Boolean = false): StickerFragment {
            val f = StickerFragment()
            val b = Bundle()
            b.putString(ARGS_ALBUM_ID, id)
            b.putBoolean(ARGS_RECENT, recent)
            f.arguments = b
            return f
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val stickerViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val albumId: String? by lazy {
        arguments!!.getString(ARGS_ALBUM_ID)
    }

    private val recent: Boolean by lazy {
        arguments!!.getBoolean(ARGS_RECENT)
    }

    private val stickers = mutableListOf<Sticker>()
    private val stickerAdapter: StickerAdapter by lazy {
        StickerAdapter(stickers)
    }

    private val padding: Int by lazy {
        context!!.dip(PADDING)
    }

    private var callback: StickerAlbumFragment.AlbumAdapter.Callback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_sticker, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (albumId != null) {
            doAsync {
                val list = stickerViewModel.getStickers(albumId!!)
                uiThread { updateStickers(list) }
            }
        } else {
            stickerViewModel.recentStickers().observe(this, Observer { r ->
                r?.let { updateStickers(r) }
            })
        }

        sticker_rv.layoutManager = GridLayoutManager(context, COLUMN)
        sticker_rv.addItemDecoration(GridSpacingItemDecoration(COLUMN, padding, true))
        stickerAdapter.size = (context!!.displaySize().x - COLUMN * padding) / COLUMN
        sticker_rv.adapter = stickerAdapter
        stickerAdapter.setOnStickerListener(object : StickerListener {
            override fun onItemClick(pos: Int, albumId: String, name: String) {
                if (!recent) {
                    stickerViewModel.updateStickerUsedAt(albumId, name)
                }
                callback?.onStickerClick(albumId, name)
            }
        })
    }

    @Synchronized
    private fun updateStickers(list: List<Sticker>) {
        if (!isAdded) return
        stickers.clear()
        stickers.addAll(list)
        stickerAdapter.notifyDataSetChanged()
    }

    fun setCallback(callback: StickerAlbumFragment.AlbumAdapter.Callback) {
        this.callback = callback
    }

    private class StickerAdapter(private val stickers: List<Sticker>) : RecyclerView.Adapter<StickerViewHolder>() {
        private var listener: StickerListener? = null
        var size: Int = 0

        override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
            val s = stickers[position]
            val ctx = holder.itemView.context

            val params = holder.itemView.layoutParams
            params.height = size
            holder.itemView.layoutParams = params
            val item = (holder.itemView as ViewGroup).getChildAt(0) as ImageView
            Glide.with(ctx).load(s.assetUrl).apply(
                if (size <= 0) RequestOptions().dontAnimate().override(Target.SIZE_ORIGINAL)
                else RequestOptions().dontAnimate().override(size, size))
                .into(item)
            item.setOnClickListener { listener?.onItemClick(position, s.albumId, s.name) }
        }

        override fun getItemCount(): Int = stickers.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sticker, parent, false)
            return StickerViewHolder(view)
        }

        fun setOnStickerListener(onStickerListener: StickerListener) {
            listener = onStickerListener
        }
    }

    private class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int,
        private val includeEdge: Boolean
    ) :
        RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount

                if (position < spanCount) {
                    outRect.top = spacing
                }
                outRect.bottom = spacing
            } else {
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount
                if (position >= spanCount) {
                    outRect.top = spacing
                }
            }
        }
    }

    interface StickerListener {
        fun onItemClick(pos: Int, albumId: String, name: String)
    }
}