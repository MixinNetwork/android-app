package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentStickerAlbumBinding
import one.mixin.android.databinding.TabAlbumStoreBinding
import one.mixin.android.event.AlbumEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.RefreshStickerAlbumJob.Companion.PREF_NEW_ALBUM
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.StickerAlbumAdapter
import one.mixin.android.ui.conversation.adapter.StickerAlbumAdapter.Companion.TYPE_RECENT
import one.mixin.android.ui.conversation.adapter.StickerAlbumAdapter.Companion.TYPE_STORE
import one.mixin.android.ui.sticker.StickerStoreActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.giphy.Image
import one.mixin.android.widget.DraggableRecyclerView
import one.mixin.android.widget.viewpager2.SwipeControlTouchListener
import one.mixin.android.widget.viewpager2.SwipeDirection
import org.jetbrains.anko.backgroundResource

@AndroidEntryPoint
class StickerAlbumFragment : BaseFragment(R.layout.fragment_sticker_album) {

    companion object {
        const val TAG = "StickerAlbumFragment"

        fun newInstance() = StickerAlbumFragment()
    }

    private val stickerViewModel by viewModels<ConversationViewModel>()

    private val albums = mutableListOf<StickerAlbum>()

    private val albumAdapter: StickerAlbumAdapter by lazy {
        StickerAlbumAdapter(this, albums).apply {
            callback = this@StickerAlbumFragment.callback
        }
    }
    private var callback: Callback? = null
    var rvCallback: DraggableRecyclerView.Callback? = null

    private val binding by viewBinding(FragmentStickerAlbumBinding::bind)
    private var _storeBinding: TabAlbumStoreBinding? = null

    private var first = true

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            stickerViewModel.observeSystemAddedAlbums().observe(
                viewLifecycleOwner
            ) { r ->
                r?.let {
                    albums.clear()
                    albums.addAll(r)
                    albumAdapter.notifyDataSetChanged()
                    context?.let { c ->
                        for (i in 1 until albumAdapter.itemCount) {
                            val tabView = albumAdapter.getTabView(i, c) as FrameLayout
                            albumTl.getTabAt(i)?.customView = tabView
                            if (albumTl.selectedTabPosition == i) {
                                tabView.setBackgroundResource(R.drawable.bg_sticker_tab)
                            }
                        }

                        val slidingTabStrip = albumTl.getChildAt(0) as ViewGroup
                        for (i in 1 until slidingTabStrip.childCount) {
                            val v = slidingTabStrip.getChildAt(i)
                            v.backgroundResource = 0
                        }
                    }

                    if (first) {
                        first = false
                        viewPager.setCurrentItem(TYPE_RECENT, false)
                    }
                }
            }
            albumAdapter.rvCallback = object : DraggableRecyclerView.Callback {
                override fun onScroll(dis: Float) {
                    rvCallback?.onScroll(dis)
                }

                override fun onRelease(fling: Int) {
                    rvCallback?.onRelease(fling)
                }
            }
            viewPager.adapter = albumAdapter
            (binding.viewPager.getChildAt(0) as? RecyclerView)?.addOnItemTouchListener(gestureListener)
            TabLayoutMediator(
                albumTl,
                viewPager
            ) { tab, pos ->
                if (pos == TYPE_STORE) {
                    _storeBinding = TabAlbumStoreBinding.inflate(layoutInflater, null, false).apply {
                        tab.customView = root
                        root.setOnClickListener {
                            dotIv.isVisible = false
                            defaultSharedPreferences.putBoolean(PREF_NEW_ALBUM, false)
                            StickerStoreActivity.show(requireContext())
                        }
                        dotIv.isVisible = defaultSharedPreferences.getBoolean(PREF_NEW_ALBUM, false)
                    }
                }
            }.attach()
            albumTl.tabMode = TabLayout.MODE_SCROLLABLE
            albumTl.addOnTabSelectedListener(
                object : TabLayout.OnTabSelectedListener {
                    override fun onTabReselected(tab: TabLayout.Tab?) {
                        // Left empty
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab) {
                        tab.customView?.setBackgroundResource(0)
                    }

                    override fun onTabSelected(tab: TabLayout.Tab) {
                        tab.customView?.setBackgroundResource(R.drawable.bg_sticker_tab)
                    }
                }
            )
        }

        RxBus.listen(AlbumEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { albumEvent ->
                if (albumEvent.hasNew) {
                    _storeBinding?.dotIv?.isVisible = true
                }
            }
    }

    override fun onStart() {
        super.onStart()
        binding.viewPager.registerOnPageChangeCallback(onPageChangeCallback)
    }

    override fun onStop() {
        super.onStop()
        binding.viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (position == TYPE_RECENT) {
                gestureListener.direction = SwipeDirection.RIGHT
            } else {
                gestureListener.direction = SwipeDirection.ALL
            }
        }
    }

    private val gestureListener = SwipeControlTouchListener()

    interface Callback {
        fun onStickerClick(stickerId: String)
        fun onGiphyClick(image: Image, previewUrl: String)
    }
}
