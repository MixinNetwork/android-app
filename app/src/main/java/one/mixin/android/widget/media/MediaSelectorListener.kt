package one.mixin.android.widget.media

import android.net.Uri

interface MediaSelectorListener {
    fun onClick(type: Int)

    fun onQuickAttachment(uri: Uri)
}