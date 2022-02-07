package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.gesture.GestureOverlayView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentStickerAlbumBinding
import one.mixin.android.databinding.TabAlbumStoreBinding
import one.mixin.android.event.AlbumEvent
import one.mixin.android.extension.backgroundResource
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
import one.mixin.android.widget.viewpager2.TabLayoutMediator
import kotlin.math.abs

@AndroidEntryPoint
class StickerAlbumFragment : BaseFragment(R.layout.fragment_sticker_album) {

    companion object {
        const val TAG = "StickerAlbumFragment"

        fun newInstance() = StickerAlbumFragment()
    }

    private val stickerViewModel by viewModels<ConversationViewModel>()

    private val albums = mutableListOf<StickerAlbum>()

    private val albumAdapter: StickerAlbumAdapter by lazy {
        StickerAlbumAdapter(requireActivity(), albums).apply {
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
                    albumAdapter.setItems(r)

                    val slidingTabStrip = albumTl.getChildAt(0) as ViewGroup
                    for (i in 1 until slidingTabStrip.childCount) {
                        val v = slidingTabStrip.getChildAt(i)
                        v.backgroundResource = 0
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
            viewPager.isUserInputEnabled = false
            touchOverlay.addOnGestureListener(gestureOverlayListener)
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
                } else {
                    val tabView = albumAdapter.getTabView(pos, requireContext())
                    tab.customView = tabView
                    if (albumTl.selectedTabPosition == pos) {
                        tabView.setBackgroundResource(R.drawable.bg_sticker_tab)
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

    private var direction = SwipeDirection.RIGHT
    private var lastX = 0f
    private var lastY = 0f

    var changed = false

    private val gestureOverlayListener = object : GestureOverlayView.OnGestureListener {
        override fun onGestureStarted(overlay: GestureOverlayView?, event: MotionEvent?) {
            handleOnTouchEvent(event)
        }

        override fun onGesture(overlay: GestureOverlayView?, event: MotionEvent?) {
            handleOnTouchEvent(event)
        }

        override fun onGestureEnded(overlay: GestureOverlayView?, event: MotionEvent?) {
            handleOnTouchEvent(event)
        }

        override fun onGestureCancelled(overlay: GestureOverlayView?, event: MotionEvent?) {
            handleOnTouchEvent(event)
        }

        private fun handleOnTouchEvent(event: MotionEvent?): Boolean {
            if (direction == SwipeDirection.NONE)
                return false
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    changed = false
                    if (!binding.viewPager.isFakeDragging) {
                        binding.viewPager.beginFakeDrag()
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val curX = event.x
                    val curY = event.y
                    val deltaX = curX - lastX
                    val deltaY = curY - lastY
                    return if (deltaX > 0 && direction == SwipeDirection.RIGHT) {
                        false
                    } else {
                        if (abs(deltaX) > abs(deltaY)) {
                            binding.viewPager.fakeDragBy(deltaX)
                            changed = true
                        }
                        lastX = curX
                        lastY = curY
                        true
                    }
                }

                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    binding.viewPager.endFakeDrag()
                }
            }
            return true
        }
    }

    enum class SwipeDirection {
        ALL, LEFT, RIGHT, NONE
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            direction = if (position == TYPE_RECENT) {
                SwipeDirection.RIGHT
            } else {
                SwipeDirection.ALL
            }
        }
    }

    interface Callback {
        fun onStickerClick(stickerId: String, albumId: String?)
        fun onGiphyClick(image: Image, previewUrl: String)
    }
}
