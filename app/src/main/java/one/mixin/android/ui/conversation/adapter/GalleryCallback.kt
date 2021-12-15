package one.mixin.android.ui.conversation.adapter

import one.mixin.android.widget.gallery.internal.entity.Item

interface GalleryCallback {
    fun onItemClick(pos: Int, item: Item, send: Boolean)

    fun onCameraClick()
}
