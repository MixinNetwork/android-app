package one.mixin.android.ui.conversation

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.fragment_sticker_album.*
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.AlbumAdapter
import one.mixin.android.vo.StickerAlbum
import org.jetbrains.anko.backgroundResource
import javax.inject.Inject

class StickerAlbumFragment : BaseFragment() {

    companion object {
        const val TAG = "StickerAlbumFragment"

        fun newInstance() = StickerAlbumFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val stickerViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val albums = mutableListOf<StickerAlbum>()

    private val albumAdapter: AlbumAdapter by lazy {
        AlbumAdapter(requireActivity().supportFragmentManager, albums).apply {
            callback = this@StickerAlbumFragment.callback
        }
    }
    private var callback: Callback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_sticker_album, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        stickerViewModel.getSystemAlbums().observe(this, Observer { r ->
            r?.let {
                albums.clear()
                albums.addAll(r)
                albumAdapter.notifyDataSetChanged()
                context?.let { c ->
                    for (i in 0 until albumAdapter.count) {
                        val tabView = albumAdapter.getTabView(i, c) as FrameLayout
                        album_tl.getTabAt(i)?.customView = tabView
                        if (album_tl.selectedTabPosition == i) {
                            tabView.setBackgroundResource(R.drawable.bg_sticker_tab)
                        }
                    }

                    val slidingTabStrip = album_tl.getChildAt(0) as ViewGroup
                    for (i in 0 until slidingTabStrip.childCount) {
                        val v = slidingTabStrip.getChildAt(i)
                        v.backgroundResource = 0
                    }
                }
            }
        })
        view_pager.adapter = albumAdapter
        album_tl.setupWithViewPager(view_pager)
        album_tl.tabMode = TabLayout.MODE_SCROLLABLE
        view_pager.currentItem = 0
        album_tl.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Left empty
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.customView?.setBackgroundResource(0)
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.customView?.setBackgroundResource(R.drawable.bg_sticker_tab)
            }
        })
        album_tl.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val moveY = event.rawY
                    if (downY != 0f) {
                        callback?.onMove(moveY - downY)
                    }
                    downY = moveY
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    downY = 0f
                    callback?.onRelease()
                }
            }
            return@setOnTouchListener false
        }
    }

    private var downY = 0f

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    interface Callback {
        fun onStickerClick(stickerId: String)
        fun onGiphyClick(url: String)
        fun onMove(dis: Float)
        fun onRelease()
    }
}