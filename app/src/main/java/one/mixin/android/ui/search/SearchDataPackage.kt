package one.mixin.android.ui.search

import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ConversationItemMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User

class SearchDataPackage(
    var contactList: List<User>? = null,
    var assetList: List<AssetItem>? = null,
    var userList: List<User>? = null,
    var groupList: List<ConversationItemMinimal>? = null,
    var messageList: List<SearchMessageItem>? = null
) {
    fun getCount(): Int {
        return if (assetList == null && userList == null && groupList == null && messageList == null) {
            contactList?.size ?: 0
        } else {
            (assetList?.size ?: 0) + (userList?.size ?: 0) + (groupList?.size ?: 0) + (messageList?.size ?: 0)
        }
    }

    fun getItem(position: Int): Any {
        return if (assetList == null && userList == null && groupList == null && messageList == null) {
            contactList!![position]
        } else {
            when {
                position < assetList?.size ?: 0 -> assetList!![position]
                position < (assetList?.size ?: 0) + (userList?.size ?: 0) -> userList!![position - (assetList?.size
                    ?: 0)]
                position < (assetList?.size ?: 0) + (userList?.size ?: 0) + (groupList?.size ?: 0) ->
                    groupList!![position - (assetList?.size ?: 0) - (userList?.size ?: 0)]
                position < (assetList?.size ?: 0) + (userList?.size ?: 0) + (groupList?.size ?: 0) + (messageList?.size
                    ?: 0) ->
                    messageList!![position - (assetList?.size ?: 0) - (userList?.size ?: 0) - (groupList?.size ?: 0)]
                else -> throw ArrayIndexOutOfBoundsException()
            }
        }
    }

    fun isAssetEnd(position: Int): Boolean = position == (assetList?.size ?: 1) - 1

    fun isUserEnd(position: Int): Boolean = position == (assetList?.size ?: 0) + (userList?.size ?: 1) - 1

    fun isGroupEnd(position: Int): Boolean = position == (assetList?.size ?: 0) + (userList?.size
        ?: 0) + (groupList?.size ?: 1) - 1

    fun isMessageEnd(position: Int): Boolean = position == (assetList?.size ?: 0) + (userList?.size
        ?: 0) + (groupList?.size ?: 0) + (messageList?.size ?: 1) - 1
}