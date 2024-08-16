package one.mixin.android.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.User
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.vo.membershipIcon

class NameTextView : AppCompatTextView {
    private val badgeSize: Int

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.NameTextView)
        badgeSize = a.getDimensionPixelSize(R.styleable.NameTextView_badgeSize, dp14)
        val badgePadding = a.getDimensionPixelSize(R.styleable.NameTextView_badgePadding, dp4)
        compoundDrawablePadding = badgePadding
        a.recycle()
        includeFontPadding = false
    }

    fun setTextOnly(text: String?) {
        this.text = text
        setCompoundDrawables(null, null, null, null)
    }

    fun setName(user: User) {
        text = user.fullName
        setCompoundDrawables(null, null, getBadge(user), null)
    }

    fun setName(item: ConversationMinimal) {
        if (item.isGroupConversation()) {
            text = item.groupName
            setCompoundDrawables(null, null, null, null)
        } else {
            text = item.name
            setCompoundDrawables(null, null, getBadge(item), null)
        }
    }

    fun setName(message: MessageItem) {
        text = message.sharedUserFullName
        setCompoundDrawables(null, null, getBadge(message), null)
    }

    fun setName(message: ChatHistoryMessageItem) {
        text = message.sharedUserFullName
        setCompoundDrawables(null, null, getBadge(message), null)
    }

    fun setName(conversationItem: ConversationItem) {
        text = conversationItem.getConversationName()
        setCompoundDrawables(null, null, getBadge(conversationItem), null)
    }

    private val dp14 by lazy {
        context.dpToPx(14f)
    }
    private val dp4 by lazy {
        context.dpToPx(4f)
    }

    private fun getBadge(user: User): Drawable? {
        val resources = if (user.isMembership()) {
            user.membership.membershipIcon()
        } else if (user.isVerified == true) {
            R.drawable.ic_user_verified
        } else if (user.isBot()) {
            R.drawable.ic_bot
        } else {
            null
        }
        return resources?.let { res ->
            AppCompatResources.getDrawable(context, res).also { icon ->
                icon?.setBounds(0, 0, badgeSize, badgeSize)
            }
        }
    }

    private fun getBadge(item: ConversationMinimal): Drawable? {
        val resources = if (item.isMembership()) {
            item.membership.membershipIcon()
        } else if (item.ownerVerified == true) {
            R.drawable.ic_user_verified
        } else if (item.isBot()) {
            R.drawable.ic_bot
        } else {
            null
        }
        return resources?.let { res ->
            AppCompatResources.getDrawable(context, res).also { icon ->
                icon?.setBounds(0, 0, badgeSize, badgeSize)
            }
        }
    }

    private fun getBadge(item: MessageItem): Drawable? {
        val resources = if (item.isSharedMembership()) {
            item.sharedMembership.membershipIcon()
        } else if (item.sharedUserIsVerified == true) {
            R.drawable.ic_user_verified
        } else if (item.sharedUserAppId != null) {
            R.drawable.ic_bot
        } else {
            null
        }
        return resources?.let { res ->
            AppCompatResources.getDrawable(context, res).also { icon ->
                icon?.setBounds(0, 0, badgeSize, badgeSize)
            }
        }
    }

    private fun getBadge(item: ChatHistoryMessageItem): Drawable? {
        val resources = if (item.isSharedMembership()) {
            item.sharedMembership.membershipIcon()
        } else if (item.sharedUserIsVerified == true) {
            R.drawable.ic_user_verified
        } else if (item.sharedUserAppId != null) {
            R.drawable.ic_bot
        } else {
            null
        }
        return resources?.let { res ->
            AppCompatResources.getDrawable(context, res).also { icon ->
                icon?.setBounds(0, 0, badgeSize, badgeSize)
            }
        }
    }

    private fun getBadge(item: ConversationItem): Drawable? {
        val resources = if (item.isMembership()) {
            item.membership.membershipIcon()
        } else if (item.ownerVerified == true) {
            R.drawable.ic_user_verified
        } else if (item.isVerified()) {
            R.drawable.ic_user_verified
        } else if(item.isBot()){
            R.drawable.ic_bot
        } else {
            null
        }
        return resources?.let { res ->
            AppCompatResources.getDrawable(context, res).also { icon ->
                icon?.setBounds(0, 0, badgeSize, badgeSize)
            }
        }
    }
}