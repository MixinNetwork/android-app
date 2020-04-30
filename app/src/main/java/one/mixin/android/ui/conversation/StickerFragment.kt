package one.mixin.android.ui.conversation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_sticker.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.DragReleaseEvent
import one.mixin.android.extension.loadSticker
import one.mixin.android.extension.realSize
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.StickerAlbumAdapter
import one.mixin.android.ui.conversation.adapter.StickerAlbumAdapter.Companion.TYPE_LIKE
import one.mixin.android.ui.conversation.adapter.StickerAlbumAdapter.Companion.TYPE_NORMAL
import one.mixin.android.ui.conversation.adapter.StickerAlbumAdapter.Companion.TYPE_RECENT
import one.mixin.android.ui.conversation.adapter.StickerSpacingItemDecoration
import one.mixin.android.ui.sticker.StickerActivity
import one.mixin.android.util.lottie.ImageListener
import one.mixin.android.util.lottie.LottieLoader
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.isLottie
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.DraggableRecyclerView.Companion.DIRECTION_NONE
import one.mixin.android.widget.DraggableRecyclerView.Companion.DIRECTION_TOP_2_BOTTOM
import one.mixin.android.widget.RLottieDrawable
import one.mixin.android.widget.RLottieImageView
import org.jetbrains.anko.dip

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
        ViewModelProvider(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val albumId: String? by lazy {
        requireArguments().getString(ARGS_ALBUM_ID)
    }

    private val type: Int by lazy {
        requireArguments().getInt(ARGS_TYPE)
    }

    private val stickers = mutableListOf<Sticker>()
    private val stickerAdapter: StickerAdapter by lazy {
        StickerAdapter(stickers, type == TYPE_LIKE)
    }

    private val padding: Int by lazy {
        requireContext().dip(PADDING)
    }

    var rvCallback: DraggableRecyclerView.Callback? = null

    private var callback: StickerAlbumAdapter.Callback? = null
    private var personalAlbumId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        layoutInflater.inflate(R.layout.fragment_sticker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (type == TYPE_NORMAL && albumId != null) {
            stickerViewModel.observeStickers(albumId!!).observe(viewLifecycleOwner, Observer { list ->
                list?.let { updateStickers(it) }
            })
        } else {
            if (type == TYPE_RECENT) {
                stickerViewModel.recentStickers().observe(viewLifecycleOwner, Observer { r ->
                    r?.let { updateStickers(r) }
                })
            } else {
                lifecycleScope.launch {
                    if (!isAdded) return@launch

                    personalAlbumId = stickerViewModel.getPersonalAlbums()?.albumId
                    if (personalAlbumId == null) { // not add any personal sticker yet
                        stickerViewModel.observePersonalStickers()
                            .observe(viewLifecycleOwner, Observer { list ->
                                list?.let { updateStickers(it) }
                            })
                    } else {
                        stickerViewModel.observeStickers(personalAlbumId!!)
                            .observe(viewLifecycleOwner, Observer { list ->
                                list?.let { updateStickers(it) }
                            })
                    }
                }
            }
        }

        sticker_rv.layoutManager = GridLayoutManager(context, COLUMN)
        sticker_rv.addItemDecoration(StickerSpacingItemDecoration(COLUMN, padding, true))
        stickerAdapter.size = (requireContext().realSize().x - (COLUMN + 1) * padding) / COLUMN
        sticker_rv.adapter = stickerAdapter
        stickerAdapter.setOnStickerListener(object : StickerListener {
            override fun onItemClick(pos: Int, stickerId: String) {
                if (type != TYPE_RECENT) {
                    stickerViewModel.updateStickerUsedAt(stickerId)
                }
                callback?.onStickerClick(stickerId)
            }

            override fun onAddClick() {
                StickerActivity.show(requireContext(), personalAlbumId)
            }
        })
        sticker_rv.callback = object : DraggableRecyclerView.Callback {
            override fun onScroll(dis: Float) {
                rvCallback?.onScroll(dis)
            }

            override fun onRelease(fling: Int) {
                rvCallback?.onRelease(fling)
            }
        }

        RxBus.listen(DragReleaseEvent::class.java)
            .autoDispose(stopScope)
            .subscribe {
                sticker_rv.direction = if (it.isExpand) DIRECTION_TOP_2_BOTTOM else DIRECTION_NONE
            }
    }

    @Synchronized
    private fun updateStickers(list: List<Sticker>) {
        if (!isAdded) return
        stickers.clear()
        stickers.addAll(list)
        stickerAdapter.notifyDataSetChanged()
    }

    fun setCallback(callback: StickerAlbumAdapter.Callback) {
        this.callback = callback
    }

    private class StickerAdapter(
        private val stickers: List<Sticker>,
        private val needAdd: Boolean
    ) : RecyclerView.Adapter<StickerViewHolder>() {
        private var listener: StickerListener? = null
        var size: Int = 0

        override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
            val params = holder.itemView.layoutParams
            params.width = size
            params.height = size
            holder.itemView.layoutParams = params
            val ctx = holder.itemView.context
            val item = (holder.itemView as ViewGroup).getChildAt(0) as RLottieImageView
            if (position == 0 && needAdd) {
                item.updateLayoutParams<ViewGroup.LayoutParams> {
                    width = size - ctx.dip(50)
                    height = size - ctx.dip(50)
                }
                item.setImageResource(R.drawable.ic_add_stikcer)
                item.setOnClickListener { listener?.onAddClick() }
            } else {
                val s = stickers[if (needAdd) position - 1 else position]
                if (s.isLottie()) {
                    LottieLoader.fromUrl(ctx, s.assetUrl, s.assetUrl, size, size)
                        .addListener(object : ImageListener<RLottieDrawable> {
                            override fun onResult(result: RLottieDrawable) {
                                item.setAnimation(result)
                                item.playAnimation()
                                item.setAutoRepeat(true)
                            }
                    })
                } else {
                    item.loadSticker(s.assetUrl, s.assetType)
                }
                item.updateLayoutParams<ViewGroup.LayoutParams> {
                    width = size
                    height = size
                }
                item.setOnClickListener { listener?.onItemClick(position, s.stickerId) }
            }
        }

        override fun getItemCount(): Int = if (needAdd) stickers.size + 1 else stickers.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_sticker, parent, false)
            return StickerViewHolder(view)
        }

        fun setOnStickerListener(onStickerListener: StickerListener) {
            listener = onStickerListener
        }
    }

    private class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface StickerListener {
        fun onItemClick(pos: Int, stickerId: String)
        fun onAddClick()
    }
}
