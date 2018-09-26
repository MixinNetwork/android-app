package one.mixin.android.ui.conversation

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_sticker.*
import one.mixin.android.R
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.loadGif
import one.mixin.android.extension.loadSticker
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.AlbumAdapter
import one.mixin.android.ui.conversation.adapter.AlbumAdapter.Companion.TYPE_GIPHY
import one.mixin.android.ui.conversation.adapter.AlbumAdapter.Companion.TYPE_LIKE
import one.mixin.android.ui.conversation.adapter.AlbumAdapter.Companion.TYPE_NORMAL
import one.mixin.android.ui.conversation.adapter.AlbumAdapter.Companion.TYPE_RECENT
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.ui.sticker.StickerActivity
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.giphy.Gif
import org.jetbrains.anko.dip
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import retrofit2.HttpException
import timber.log.Timber
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StickerFragment : BaseFragment() {

    companion object {
        const val TAG = "StickerFragment"
        const val ARGS_ALBUM_ID = "args_album_id"
        const val ARGS_TYPE = "args_type"
        const val PADDING = 10
        const val COLUMN = 3
        const val LIMIT = 25
        const val INTERVAL = 2000

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
    private val giphys = mutableListOf<Gif>()
    private val stickerAdapter: StickerAdapter by lazy {
        StickerAdapter(stickers, giphys, type)
    }

    private var offset = 0
    private var trending = false
    private var lastTrendingTime = 0L

    private val padding: Int by lazy {
        context!!.dip(PADDING)
    }

    private var callback: AlbumAdapter.Callback? = null
    private var personalAlbumId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_sticker, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initData()
        initView()
    }

    override fun onStop() {
        super.onStop()
        offset = 0
        trending = false
    }

    private fun initView() {
        sticker_rv.layoutManager = GridLayoutManager(context, COLUMN)
        sticker_rv.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, true))
        stickerAdapter.size = (context!!.displaySize().x - (COLUMN + 1) * padding) / COLUMN
        sticker_rv.adapter = stickerAdapter
        stickerAdapter.setOnStickerListener(object : StickerListener {
            override fun onItemClick(pos: Int, s: String) {
                if (type == TYPE_GIPHY) {
                    handleClickGiphy(s)
                    return
                }
                if (type != TYPE_RECENT) {
                    stickerViewModel.updateStickerUsedAt(s)
                }
                callback?.onStickerClick(s)
            }

            override fun onAddClick() {
                StickerActivity.show(requireContext(), personalAlbumId)
            }

            override fun onSearchClick() {
                val f = GiphyBottomSheetFragment.newInstance()
                f.showNow(requireFragmentManager(), GiphyBottomSheetFragment.TAG)
                f.callback = object : GiphyBottomSheetFragment.Callback {
                    override fun onGiphyClick(url: String) {
                        handleClickGiphy(url)
                    }
                }
            }
        })
        if (type == TYPE_GIPHY) {
            sticker_rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (!trending && !sticker_rv.canScrollVertically(1)) {
                        val cur = System.currentTimeMillis()
                        if (cur - lastTrendingTime < INTERVAL) return

                        lastTrendingTime = cur
                        trending = true
                        stickerViewModel.trendingGifs(LIMIT, offset)
                            .autoDisposable(scopeProvider)
                            .subscribe({ list ->
                                offset += LIMIT
                                trending = false
                                giphys.addAll(list)
                                stickerAdapter.notifyItemRangeInserted(offset + 1, LIMIT)
                            }, { t ->
                                trending = false
                                Timber.d("Trending gifs failed, t: ${t.printStackTrace()}")
                                if (t is HttpException && t.code() == 429) {
                                    toast("Giphy API rate limit exceeded")
                                }
                            })
                    }
                }
            })
        }
    }

    private fun initData() {
        if (type == TYPE_NORMAL && albumId != null) {
            stickerViewModel.observeStickers(albumId!!).observe(this, Observer { list ->
                list?.let { updateStickers(it) }
            })
        } else if (type == TYPE_GIPHY) {
            stickerViewModel.trendingGifs(LIMIT, offset)
                .autoDisposable(scopeProvider)
                .subscribe({ list ->
                    if (!isAdded) return@subscribe
                    giphys.clear()
                    giphys.addAll(list)
                    offset += LIMIT
                    stickerAdapter.notifyDataSetChanged()
                }, { t ->
                    Timber.d("Trending gifs failed, t: ${t.printStackTrace()}")
                    if (t is HttpException && t.code() == 429) {
                        toast("Giphy API rate limit exceeded")
                    }
                })
        } else {
            if (type == TYPE_RECENT) {
                stickerViewModel.recentStickers()
                    .observe(this, Observer { r ->
                        r?.let { updateStickers(r) }
                    })
            } else {
                doAsync {
                    personalAlbumId = stickerViewModel.getPersonalAlbums()?.albumId

                    uiThread { _ ->
                        if (personalAlbumId == null) { // not add any personal sticker yet
                            stickerViewModel.observePersonalStickers()
                                .observe(this@StickerFragment, Observer { list ->
                                    list?.let { updateStickers(it) }
                                })
                        } else {
                            stickerViewModel.observeStickers(personalAlbumId!!)
                                .observe(this@StickerFragment, Observer { list ->
                                    list?.let { updateStickers(it) }
                                })
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    private fun updateStickers(list: List<Sticker>) {
        if (!isAdded) return
        stickers.clear()
        stickers.addAll(list)
        stickerAdapter.notifyDataSetChanged()
    }

    private fun handleClickGiphy(s: String) {
        doAsync {
            val f = try {
                Glide.with(requireContext()).downloadOnly().load(s).submit().get(10, TimeUnit.SECONDS)
            } catch (e: Exception) {
                return@doAsync
            }
            uiThread {
                val file = requireContext().getImagePath().createGifTemp()
                file.copyFromInputStream(FileInputStream(f))
                if (file.absolutePath != null && f.length() > 0) {
                    callback?.onGiphyClick(file.absolutePath)
                } else {
                    requireContext().toast("Send giphy image failed.")
                }
            }
        }
    }

    fun setCallback(callback: AlbumAdapter.Callback) {
        this.callback = callback
    }

    private class StickerAdapter(
        private val stickers: List<Sticker>,
        private val giphys: List<Gif>,
        private val type: Int)
        : RecyclerView.Adapter<StickerViewHolder>() {
        private var listener: StickerListener? = null
        var size: Int = 0

        override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
            val params = holder.itemView.layoutParams
            params.width = size
            params.height = size
            holder.itemView.layoutParams = params
            val ctx = holder.itemView.context
            val item = (holder.itemView as ViewGroup).getChildAt(0) as ImageView
            if (position == 0 && needOp()) {
                item.updateLayoutParams<ViewGroup.LayoutParams> {
                    width = size - ctx.dip(50)
                    height = size - ctx.dip(50)
                }
                if (type == TYPE_LIKE) {
                    item.setImageResource(R.drawable.ic_add_stikcer)
                    item.setOnClickListener { listener?.onAddClick() }
                } else {
                    item.setImageResource(R.drawable.ic_gif_search)
                    item.setOnClickListener { listener?.onSearchClick() }
                }
            } else {
                if (type == TYPE_GIPHY) {
                    val g = giphys[if (needOp()) position - 1 else position].images.fixed_width
                    item.loadGif(g.url)
                    item.setOnClickListener { listener?.onItemClick(position, g.url) }
                } else {
                    val s = stickers[if (needOp()) position - 1 else position]
                    item.loadSticker(s.assetUrl, s.assetType)
                    item.setOnClickListener { listener?.onItemClick(position, s.stickerId) }
                }
                item.updateLayoutParams<ViewGroup.LayoutParams> {
                    width = size
                    height = size
                }
            }
        }

        override fun getItemCount(): Int = if (needOp()) {
            if (type == TYPE_GIPHY) {
                giphys.size + 1
            } else {
                stickers.size + 1
            }
        } else {
            if (type == TYPE_GIPHY) {
                giphys.size
            } else {
                stickers.size
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sticker, parent, false)
            return StickerViewHolder(view)
        }

        private fun needOp() = type == TYPE_LIKE || type == TYPE_GIPHY

        fun setOnStickerListener(onStickerListener: StickerListener) {
            listener = onStickerListener
        }
    }

    private class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface StickerListener {
        fun onItemClick(pos: Int, s: String)
        fun onAddClick()
        fun onSearchClick()
    }
}