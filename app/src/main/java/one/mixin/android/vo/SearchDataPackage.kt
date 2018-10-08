package one.mixin.android.vo

data class SearchDataPackage(
    val contactList: List<User>?,
    val assetList: List<AssetItem>?,
    val userList: List<User>?,
    val groupList: List<ConversationItemMinimal>?,
    val messageList: List<SearchMessageItem>?
)