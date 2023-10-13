package one.mixin.android.ui.media.pager.transcript

import android.view.View
import one.mixin.android.vo.ChatHistoryMessageItem

interface MediaPagerAdapterListener {
    fun onClick(messageItem: ChatHistoryMessageItem)

    fun onLongClick(messageItem: ChatHistoryMessageItem, view: View)

    fun onCircleProgressClick(messageItem: ChatHistoryMessageItem)

    fun onReadyPostTransition(view: View)

    fun switchToPin(messageItem: ChatHistoryMessageItem, view: View)

    fun finishAfterTransition()

    fun switchFullscreen()
}
