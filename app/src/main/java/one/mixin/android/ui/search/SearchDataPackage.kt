package one.mixin.android.ui.search

import one.mixin.android.ui.search.holder.TipItem
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import kotlin.math.min

class SearchDataPackage(
    var assetList: List<AssetItem>? = null,
    var userList: List<User>? = null,
    var chatList: List<ChatMinimal>? = null,
    var messageList: List<SearchMessageItem>? = null
) {

    companion object {
        const val LIMIT_COUNT = 3
    }

    var showTip = false
    var assetLimit = true
    var userLimit = true
    var chatLimit = true
    var messageLimit = true

    fun getHeaderFactor(position: Int) =
        when (getItem(position)) {
            is TipItem -> 0
            is AssetItem -> if (assetShowMore()) 10 else 0
            is User -> if (userShowMore()) 10 else 0
            is ChatMinimal -> if (chatShowMore()) 10 else 0
            else -> if (messageShowMore()) 10 else 0
        }

    fun assetShowMore() = if (assetList == null || !assetLimit) {
        false
    } else {
        assetLimit && assetList!!.size > LIMIT_COUNT
    }

    fun userShowMore() = if (userList == null || !userLimit) {
        false
    } else {
        userLimit && userList!!.size > LIMIT_COUNT
    }

    fun chatShowMore() = if (chatList == null || !chatLimit) {
        false
    } else {
        chatLimit && chatList!!.size > LIMIT_COUNT
    }

    fun messageShowMore() = if (messageList == null || !messageLimit) {
        false
    } else {
        messageLimit && messageList!!.size > LIMIT_COUNT
    }

    private fun assetCount() = if (assetLimit) {
        min(assetList?.size ?: 0, LIMIT_COUNT)
    } else {
        assetList?.size ?: 0
    }

    private fun userCount() = if (userLimit) {
        min(userList?.size ?: 0, LIMIT_COUNT)
    } else {
        userList?.size ?: 0
    }

    fun chatCount() = if (chatLimit) {
        min(chatList?.size ?: 0, LIMIT_COUNT)
    } else {
        chatList?.size ?: 0
    }

    fun messageCount() = if (messageLimit) {
        min(messageList?.size ?: 0, LIMIT_COUNT)
    } else {
        messageList?.size ?: 0
    }

    fun getCount() = assetCount() + chatCount() + userCount() + messageCount().incTip()

    private fun assetItem(position: Int): AssetItem {
        return assetList!![position.decTip()]
    }

    private fun userItem(position: Int): User {
        return userList!![position.decTip() - assetCount()]
    }

    private fun chatItem(position: Int): ChatMinimal {
        return chatList!![position.decTip() - assetCount() - userCount()]
    }

    private fun messageItem(position: Int): SearchMessageItem {
        return messageList!![position.decTip() - assetCount() - userCount() - chatCount()]
    }

    fun getItem(position: Int): Any {
        return when {
            showTip && position < 1 -> TipItem()
            position < assetCount().incTip() -> assetItem(position)
            position < assetCount().incTip() + userCount() -> userItem(position)
            position < assetCount().incTip() + userCount() + chatCount() -> chatItem(position)
            else -> messageItem(position)
        }
    }

    private fun Int.incTip() = this + if (showTip) 1 else 0
    private fun Int.decTip() = this - if (showTip) 1 else 0
}
