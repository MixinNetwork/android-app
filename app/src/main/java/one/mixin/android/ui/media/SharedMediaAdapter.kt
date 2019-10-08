package one.mixin.android.ui.media

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import one.mixin.android.R

class SharedMediaAdapter(
    fm: FragmentManager,
    private val context: Context,
    private val conversationId: String
) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    companion object {
        private const val TAG_COUNT = 4
    }

    override fun getItem(position: Int) = when (position) {
        0 -> MediaFragment.newInstance(conversationId)
        1 -> AudioFragment.newInstance()
        2 -> LinkFragment.newInstance()
        else -> FileFragment.newInstance()
    }

    override fun getCount() = TAG_COUNT

    override fun getPageTitle(position: Int) = context.getString(
        when (position) {
            0 -> R.string.media
            1 -> R.string.audio
            2 -> R.string.links
            else -> R.string.files
        }
    )
}
