package one.mixin.android.ui.conversation

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.fragment_sticker_album.*
import kotlinx.android.synthetic.main.layout_sticker_tab.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.vo.StickerAlbum
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
        AlbumAdapter(activity!!.supportFragmentManager, albums).apply {
            callback = this@StickerAlbumFragment.callback
        }
    }
    private var callback: Callback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_sticker_album, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        stickerViewModel.getStickerAlbums().observe(this, Observer { r ->
            r?.let {
                albums.clear()
                albums.addAll(r)
                albumAdapter.notifyDataSetChanged()
                context?.let { c ->
                    for (i in 0..albums.size) {
                        val tabView = albumAdapter.getTabView(i, c) as FrameLayout
                        album_tl.getTabAt(i)?.customView = tabView
                        if (album_tl.selectedTabPosition == i) {
                            tabView.setBackgroundResource(R.drawable.bg_sticker_tab)
                        }
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

    class AlbumAdapter(fm: FragmentManager, private val albums: List<StickerAlbum>) : FragmentPagerAdapter(fm) {
        var callback: StickerAlbumFragment.Callback? = null

        override fun getItem(position: Int): Fragment {
            val stickerFragment = if (position == 0) {
                StickerFragment.newInstance(recent = true)
            } else {
                StickerFragment.newInstance(albums[position - 1].albumId)
            }
            stickerFragment.setCallback(object : Callback {
                override fun onStickerClick(albumId: String, name: String) {
                    callback?.onStickerClick(albumId, name)
                }
            })
            return stickerFragment
        }

        override fun getCount(): Int = albums.size + 1

        fun getTabView(pos: Int, context: Context): View {
            val view = View.inflate(context, R.layout.layout_sticker_tab, null)
            if (pos == 0) {
                view.icon.setImageResource(R.drawable.ic_access_time_gray_24dp)
            } else {
                view.icon.loadImage(albums[pos - 1].iconUrl)
            }
            return view
        }

        interface Callback {
            fun onStickerClick(albumId: String, name: String)
        }
    }

    interface Callback {
        fun onStickerClick(albumId: String, name: String)

        fun onMove(dis: Float)

        fun onRelease()
    }
}