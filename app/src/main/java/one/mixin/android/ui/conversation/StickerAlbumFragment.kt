package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentStickerAlbumBinding
import one.mixin.android.event.AlbumEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.job.RefreshStickerAlbumJob.Companion.PREF_NEW_ALBUM
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.adapter.StickerAlbumAdapter
import one.mixin.android.ui.sticker.StickerStoreActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.giphy.Image
import one.mixin.android.widget.DraggableRecyclerView
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
                        for (i in 0 until albumAdapter.itemCount) {
                            val tabView = albumAdapter.getTabView(i, c) as FrameLayout
                            albumTl.getTabAt(i)?.customView = tabView
                            if (albumTl.selectedTabPosition == i) {
                                tabView.setBackgroundResource(R.drawable.bg_sticker_tab)
                            }
                        }

                        val slidingTabStrip = albumTl.getChildAt(0) as ViewGroup
                        for (i in 0 until slidingTabStrip.childCount) {
                            val v = slidingTabStrip.getChildAt(i)
                            v.backgroundResource = 0
                        }
                    }
                }
            }
            storeFl.setOnClickListener {
                dotIv.isVisible = false
                defaultSharedPreferences.putBoolean(PREF_NEW_ALBUM, false)
                StickerStoreActivity.show(requireContext())
            }
            dotIv.isVisible = defaultSharedPreferences.getBoolean(PREF_NEW_ALBUM, false)
            albumAdapter.rvCallback = object : DraggableRecyclerView.Callback {
                override fun onScroll(dis: Float) {
                    rvCallback?.onScroll(dis)
                }

                override fun onRelease(fling: Int) {
                    rvCallback?.onRelease(fling)
                }
            }
            viewPager.adapter = albumAdapter
            TabLayoutMediator(
                albumTl,
                viewPager
            ) { tab, _ ->
                viewPager.setCurrentItem(tab.position, true)
            }.attach()
            albumTl.tabMode = TabLayout.MODE_SCROLLABLE
            viewPager.currentItem = 0
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
                    binding.dotIv.isVisible = true
                }
            }
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    interface Callback {
        fun onStickerClick(stickerId: String)
        fun onGiphyClick(image: Image, previewUrl: String)
    }
}
