package one.mixin.android.ui.search

import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import kotlin.math.min

class SearchDataPackage(
    var contactList: List<User>? = null,
    var assetList: List<AssetItem>? = null,
    var userList: List<User>? = null,
    var chatList: List<ChatMinimal>? = null,
    var messageList: List<SearchMessageItem>? = null
) {

    companion object {
        const val LIMIT_COUNT = 3
    }

    var assetLimit = true
    var userLimit = true
    var chatLimit = true
    var messageLimit = true

    fun assetShowMore(): Boolean {
        return if (assetList == null) {
            false
        } else {
            assetLimit && assetList!!.size > LIMIT_COUNT
        }
    }

    fun userShowMore(): Boolean {
        return if (userList == null) {
            false
        } else {
            userLimit && userList!!.size > LIMIT_COUNT
        }
    }

    fun chatShowMore(): Boolean {
        return if (chatList == null) {
            false
        } else {
            chatLimit && chatList!!.size > LIMIT_COUNT
        }
    }

    fun messageShowMore(): Boolean {
        return if (contactList == null) {
            false
        } else {
            messageLimit && contactList!!.size > LIMIT_COUNT
        }
    }

    private fun assetCount(): Int {
        return if (assetLimit) {
            min(assetList?.size ?: 0, LIMIT_COUNT)
        } else {
            assetList?.size ?: 0
        }
    }

    private fun userCount(): Int {
        return if (userLimit) {
            min(userList?.size ?: 0, LIMIT_COUNT)
        } else {
            userList?.size ?: 0
        }
    }

    private fun chatCount(): Int {
        return if (chatLimit) {
            min(chatList?.size ?: 0, LIMIT_COUNT)
        } else {
            chatList?.size ?: 0
        }
    }

    private fun messageCount(): Int {
        return if (messageLimit) {
            min(messageList?.size ?: 0, LIMIT_COUNT)
        } else {
            messageList?.size ?: 0
        }
    }

    fun getCount(): Int {
        return if (assetList == null && chatList == null && userList == null && messageList == null) {
            contactList?.size ?: 0
        } else {
            assetCount() + chatCount() + userCount() + messageCount()
        }
    }

    private fun assetItem(position: Int): AssetItem {
        return assetList!![position]
    }

    private fun chatItem(position: Int): ChatMinimal {
        return chatList!![position - assetCount()]
    }

    private fun userItem(position: Int): User {
        return userList!![position - assetCount() + chatCount()]
    }

    private fun messageItem(position: Int): SearchMessageItem {
        return messageList!![position - assetCount() - chatCount() - userCount()]
    }

    fun getItem(position: Int): Any {
        return if (assetList == null && chatList == null && userList == null && messageList == null) {
            contactList!![position]
        } else {
            when {
                position < assetCount() -> assetItem(position)
                position < assetCount() + chatCount() -> chatItem(position)
                position < assetCount() + chatCount() + userCount() -> userItem(position)
                position < assetCount() + chatCount() + userCount() + messageCount() -> messageItem(position)
                else -> throw ArrayIndexOutOfBoundsException()
            }
        }
    }
}