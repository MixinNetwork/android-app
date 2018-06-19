package one.mixin.android.ui.conversation

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
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
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.displaySize
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.AlbumAdapter
import one.mixin.android.ui.conversation.adapter.AlbumAdapter.Companion.TYPE_LIKE
import one.mixin.android.ui.conversation.adapter.AlbumAdapter.Companion.TYPE_NORMAL
import one.mixin.android.ui.conversation.adapter.AlbumAdapter.Companion.TYPE_RECENT
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.vo.Sticker
import org.jetbrains.anko.dip
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import javax.inject.Inject

class StickerFragment : BaseFragment() {

    companion object {
        const val TAG = "StickerFragment"
        const val ARGS_ALBUM_ID = "args_album_id"
        const val ARGS_TYPE = "args_type"
        const val PADDING = 10
        const val COLUMN = 3

        fun newInstance(id: String? = null, type: Int): StickerFragment {
            val f = StickerFragment()
            val b = Bundle()
            b.putString(ARGS_ALBUM_ID, id)
            b.putInt(ARGS_TYPE, type)
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

    private val type: Int by lazy {
        arguments!!.getInt(ARGS_TYPE)
    }

    private val stickers = mutableListOf<Sticker>()
    private val stickerAdapter: StickerAdapter by lazy {
        StickerAdapter(stickers, type == TYPE_LIKE)
    }

    private val padding: Int by lazy {
        context!!.dip(PADDING)
    }

    private var callback: AlbumAdapter.Callback? = null
    private var personalAlbumId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_sticker, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (type == TYPE_NORMAL && albumId != null) {
            doAsync {
                val list = stickerViewModel.getStickers(albumId!!)
                uiThread { updateStickers(list) }
            }
        } else {
            if (type == TYPE_RECENT) {
                stickerViewModel.recentStickers().observe(this, Observer { r ->
                    r?.let { updateStickers(r) }
                })
            } else {
                doAsync {
                    val personalAlbum = stickerViewModel.getPersonalAlbums()
                    personalAlbum?.let {
                        personalAlbumId = it.albumId
                        val list = stickerViewModel.getStickers(it.albumId)
                        uiThread { updateStickers(list) }
                    }
                }
            }
        }

        sticker_rv.layoutManager = GridLayoutManager(context, COLUMN)
        sticker_rv.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, true))
        stickerAdapter.size = (context!!.displaySize().x - COLUMN * padding) / COLUMN
        sticker_rv.adapter = stickerAdapter
        stickerAdapter.setOnStickerListener(object : StickerListener {
            override fun onItemClick(pos: Int, albumId: String, name: String) {
                if (type != TYPE_RECENT) {
                    stickerViewModel.updateStickerUsedAt(albumId, name)
                }
                callback?.onStickerClick(albumId, name)
            }

            override fun onAddClick() {
                personalAlbumId?.let {
                    requireActivity().addFragment(this@StickerFragment,
                        StickerManagementFragment.newInstance(it), StickerManagementFragment.TAG)
                }
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

    fun setCallback(callback: AlbumAdapter.Callback) {
        this.callback = callback
    }

    private class StickerAdapter(private val stickers: List<Sticker>, private val needAdd: Boolean) : RecyclerView.Adapter<StickerViewHolder>() {
        private var listener: StickerListener? = null
        var size: Int = 0

        override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
            val params = holder.itemView.layoutParams
            params.height = size
            holder.itemView.layoutParams = params
            val item = (holder.itemView as ViewGroup).getChildAt(0) as ImageView
            if (position == 0 && needAdd) {
                item.setImageResource(R.drawable.ic_add_stikcer)
                item.setOnClickListener { listener?.onAddClick() }
            } else {
                val s = stickers[position]
                val ctx = holder.itemView.context
                Glide.with(ctx).load(s.assetUrl).apply(
                    if (size <= 0) RequestOptions().dontAnimate().override(Target.SIZE_ORIGINAL)
                    else RequestOptions().dontAnimate().override(size, size))
                    .into(item)
                item.setOnClickListener { listener?.onItemClick(position, s.albumId, s.name) }
            }
        }

        override fun getItemCount(): Int = if (needAdd) stickers.size + 1 else stickers.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sticker, parent, false)
            return StickerViewHolder(view)
        }

        fun setOnStickerListener(onStickerListener: StickerListener) {
            listener = onStickerListener
        }
    }

    private class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface StickerListener {
        fun onItemClick(pos: Int, albumId: String, name: String)
        fun onAddClick()
    }
}