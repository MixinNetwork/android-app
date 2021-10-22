package one.mixin.android.ui.conversation.adapter

import android.net.Uri

interface GalleryCallback {
    fun onItemClick(pos: Int, uri: Uri, isVideo: Boolean, send: Boolean)

    fun onCameraClick()
}
