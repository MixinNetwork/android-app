package one.mixin.android.ui.media.pager.transcript

import android.view.View
import one.mixin.android.vo.TranscriptMessageItem

interface MediaPagerAdapterListener {
    fun onClick(messageItem: TranscriptMessageItem)

    fun onLongClick(messageItem: TranscriptMessageItem, view: View)

    fun onCircleProgressClick(messageItem: TranscriptMessageItem)

    fun onReadyPostTransition(view: View)

    fun switchToPin(messageItem: TranscriptMessageItem, view: View)

    fun finishAfterTransition()

    fun switchFullscreen()
}
