package one.mixin.android.vo

interface IConversationCategory {
    val conversationCategory: String
}

fun IConversationCategory.isGroupConversation() = conversationCategory == ConversationCategory.GROUP.name

fun IConversationCategory.isContactConversation() = conversationCategory == ConversationCategory.CONTACT.name
