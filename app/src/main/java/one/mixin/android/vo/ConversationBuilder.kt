package one.mixin.android.vo

class ConversationBuilder(val conversationId: String, val createdAt: String, val status: Int) {
    private var ownerId: String? = null
    private var category: String? = null
    private var name: String? = null
    private var iconUrl: String? = null
    private var announcement: String? = null
    private var codeUrl: String? = null
    private var payType: String? = null
    private var pinTime: String? = null
    private var lastMessageId: String? = null
    private var lastReadMessageId: String? = null
    private var unseenMessageCount: Int? = null
    private var draft: String? = null
    private var muteUntil: String? = null
    private var expireIn: Long? = null

    fun setOwnerId(ownerId: String): ConversationBuilder {
        this.ownerId = ownerId
        return this
    }

    fun setCategory(category: String?): ConversationBuilder {
        this.category = category
        return this
    }

    fun setName(name: String): ConversationBuilder {
        this.name = name
        return this
    }

    fun setIconUrl(iconUrl: String): ConversationBuilder {
        this.iconUrl = iconUrl
        return this
    }

    fun setAnnouncement(announcement: String): ConversationBuilder {
        this.announcement = announcement
        return this
    }

    fun setCodeUrl(codeUrl: String): ConversationBuilder {
        this.codeUrl = codeUrl
        return this
    }

    fun setPayType(payType: String): ConversationBuilder {
        this.payType = payType
        return this
    }

    fun setPinTime(pinTime: String): ConversationBuilder {
        this.pinTime = pinTime
        return this
    }

    fun setLastMessageId(lastMessageId: String): ConversationBuilder {
        this.lastMessageId = lastMessageId
        return this
    }

    fun setLastReadMessageId(lastReadMessageId: String): ConversationBuilder {
        this.lastReadMessageId = lastReadMessageId
        return this
    }

    fun setUnseenMessageCount(unseenMessageCount: Int): ConversationBuilder {
        this.unseenMessageCount = unseenMessageCount
        return this
    }

    fun setDraft(draft: String): ConversationBuilder {
        this.draft = draft
        return this
    }

    fun setMuteUntil(muteUntil: String): ConversationBuilder {
        this.muteUntil = muteUntil
        return this
    }

    fun setExpireIn(expireIn: Long?): ConversationBuilder {
        this.expireIn = expireIn
        return this
    }

    fun build(): Conversation =
        Conversation(
            conversationId, ownerId, category,
            name, iconUrl, announcement, codeUrl, payType,
            createdAt, pinTime, lastMessageId, lastReadMessageId,
            unseenMessageCount, status, draft, muteUntil, null, expireIn
        )
}
