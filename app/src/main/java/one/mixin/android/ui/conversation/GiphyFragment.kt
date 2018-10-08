package one.mixin.android.ui.conversation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_sticker.*
import one.mixin.android.R
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.loadGif
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.headrecyclerview.FooterAdapter
import one.mixin.android.ui.conversation.StickerFragment.Companion.COLUMN
import one.mixin.android.ui.conversation.adapter.AlbumAdapter
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.vo.giphy.Gif
import org.jetbrains.anko.dip
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import retrofit2.HttpException
import timber.log.Timber
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GiphyFragment : BaseFragment() {
    companion object {
        const val TAG = "GiphyFragment"
        fun newInstance() = GiphyFragment()
    }

    private val padding: Int by lazy {
        context!!.dip(StickerFragment.PADDING)
    }
    private val giphyAdapter: GiphyAdapter by lazy { GiphyAdapter() }
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val stickerViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }
    var callback: AlbumAdapter.Callback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_sticker, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        sticker_rv.layoutManager = GridLayoutManager(context, COLUMN).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(pos: Int): Int {
                    return if (pos == giphyAdapter.itemCount - 1) {
                        COLUMN
                    } else {
                        1
                    }
                }
            }
        }
        val foot = layoutInflater.inflate(R.layout.view_giphy_foot, sticker_rv, false)
        giphyAdapter.footerView = foot
        sticker_rv.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, true))
        giphyAdapter.size = (context!!.displaySize().x - (COLUMN + 1) * padding) / COLUMN
        sticker_rv.adapter = giphyAdapter
        giphyAdapter.setOnGiphyListener(object : GiphyListener {
            override fun onItemClick(pos: Int, s: String) {
                handleClickGiphy(s)
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
        stickerViewModel.trendingGifs(26, 0)
            .autoDisposable(scopeProvider)
            .subscribe({ list ->
                if (!isAdded) return@subscribe
                giphyAdapter.data = list
                giphyAdapter.notifyDataSetChanged()
            }, { t ->
                Timber.d("Trending gifs failed, t: ${t.printStackTrace()}")
                if (t is HttpException && t.code() == 429) {
                    toast("Giphy API rate limit exceeded")
                }
            })
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

    private class GiphyAdapter : FooterAdapter<Gif>() {
        private var listener: GiphyListener? = null
        var size: Int = 0

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position == itemCount - 1) {
                return
            }

            val params = holder.itemView.layoutParams
            params.width = size
            params.height = size
            holder.itemView.layoutParams = params
            val ctx = holder.itemView.context
            val item = (holder.itemView as ViewGroup).getChildAt(0) as ImageView
            if (position == 0) {
                item.updateLayoutParams<FrameLayout.LayoutParams> {
                    width = size - ctx.dip(20)
                    height = size - ctx.dip(20)
                    this.topMargin = ctx.dip(10)
                }
                item.setImageResource(R.drawable.ic_gif_search)
                item.setOnClickListener { listener?.onSearchClick() }
            } else {
                val g = data!![position - 1].images.fixed_width
                item.loadGif(g.url)
                item.setOnClickListener { listener?.onItemClick(position, g.url) }
                item.updateLayoutParams<ViewGroup.LayoutParams> {
                    width = size
                    height = size
                }
            }
        }

        override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sticker, parent, false)
            return NormalHolder(view)
        }

        override fun getItemCount(): Int = notNullElse(data, {
            if (footerView != null) it.size + 2 else it.size + 1
        }, 0)

        fun setOnGiphyListener(giphyListener: GiphyListener) {
            listener = giphyListener
        }
    }

    interface GiphyListener {
        fun onItemClick(pos: Int, s: String)
        fun onSearchClick()
    }
}