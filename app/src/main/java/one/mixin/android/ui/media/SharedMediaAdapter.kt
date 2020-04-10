package one.mixin.android.ui.media

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SharedMediaAdapter(
    activity: FragmentActivity,
    private val conversationId: String
) : FragmentStateAdapter(activity) {
    companion object {
        private const val TAB_COUNT = 5
    }

    override fun getItemCount() = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MediaFragment.newInstance(conversationId)
            1 -> AudioFragment.newInstance(conversationId)
            2 -> PostFragment.newInstance(conversationId)
            3 -> LinkFragment.newInstance(conversationId)
            else -> FileFragment.newInstance(conversationId)
        }
    }
}
