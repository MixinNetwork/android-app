package one.mixin.android.util

import android.arch.paging.PagedList
import one.mixin.android.vo.MessageItem

class DataPackage constructor(
    val data: PagedList<MessageItem>,
    val index: Int,
    val hasUnread: Boolean,
    val isStranger: Boolean = false
)
