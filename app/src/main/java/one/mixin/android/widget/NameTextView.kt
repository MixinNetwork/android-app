package one.mixin.android.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.User
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.vo.membershipIcon

class NameTextView : AppCompatTextView {
    private val badgeSize: Int

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.NameTextView)
        badgeSize = a.getDimensionPixelSize(R.styleable.NameTextView_badgeSize, dp12)
        val badgePadding = a.getDimensionPixelSize(R.styleable.NameTextView_badgePadding, dp8)
        compoundDrawablePadding = badgePadding
        a.recycle()
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

    private val dp12 by lazy {
        context.dpToPx(12f)
    }
    private val dp8 by lazy {
        context.dpToPx(8f)
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
        return resources?.let { it ->
            AppCompatResources.getDrawable(context, it).also { icon ->
                icon?.setBounds(0, 0, dp12, dp12)
            }
        }
    }

    private fun getBadge(item: ConversationMinimal): Drawable? {
        val resources = if (item.isMembership()) {
            item.membership.membershipIcon()
        } else if (item.isBot()) { // todo maybe verified icon
            R.drawable.ic_bot
        } else {
            null
        }
        return resources?.let { it ->
            AppCompatResources.getDrawable(context, it).also { icon ->
                icon?.setBounds(0, 0, dp12, dp12)
            }
        }
    }
}