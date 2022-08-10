package one.mixin.android.ui.media

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class SharedMediaAdapter(
    fragment: Fragment,
    private val conversationId: String,
    private val longClickListener: (String) -> Unit
) : FragmentStateAdapter(fragment) {
    companion object {
        private const val TAB_COUNT = 5
    }

    override fun getItemCount() = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MediaFragment.newInstance(conversationId).apply {
                this.longClickListener = this@SharedMediaAdapter.longClickListener
            }
            1 -> AudioFragment.newInstance(conversationId).apply {
                this.longClickListener = this@SharedMediaAdapter.longClickListener
            }
            2 -> PostFragment.newInstance(conversationId).apply {
                this.longClickListener = this@SharedMediaAdapter.longClickListener
            }
            3 -> LinkFragment.newInstance(conversationId)
            else -> FileFragment.newInstance(conversationId).apply {
                this.longClickListener = this@SharedMediaAdapter.longClickListener
            }
        }
    }
}
