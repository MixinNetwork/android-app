package one.mixin.android.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import one.mixin.android.R
import one.mixin.android.databinding.ViewNameTextBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.highLight
import one.mixin.android.vo.Account
import one.mixin.android.vo.CallUser
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.ExploreApp
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.ParticipantItem
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.UserItem
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.vo.membershipIcon
import one.mixin.android.widget.lottie.RLottieDrawable

class NameTextView : LinearLayoutCompat {
    private val badgeSize: Int

    private val binding = ViewNameTextBinding.inflate(LayoutInflater.from(context), this)
    val textView get() = binding.text
    private val iconView get() = binding.icon

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.NameTextView)
        val badgePadding = a.getDimensionPixelSize(R.styleable.NameTextView_badgePadding, dp4)
        badgeSize = a.getDimensionPixelSize(R.styleable.NameTextView_badgeSize, dp14)
        textView.compoundDrawablePadding = badgePadding
        iconView.updateLayoutParams<MarginLayoutParams> {
            width = badgeSize
            height = badgeSize
            marginStart = badgePadding
        }
        a.recycle()
        textView.includeFontPadding = false
    }

    fun setTextColor(color: Int) {
        this.textView.setTextColor(color)
    }

    fun setTextOnly(text: String?) {
        this.textView.text = text
        iconView.isVisible = false
        iconView.stopAnimation()
        this.textView.setCompoundDrawables(null, null, null, null)
    }

    fun setTextOnly(@StringRes text: Int) {
        textView.setText(text)
        iconView.isVisible = false
        iconView.stopAnimation()
        this.textView.setCompoundDrawables(null, null, null, null)
    }

    fun setName(user: User) {
        this.textView.text = user.fullName
        if (user.isProsperity()) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(user), null)
    }

    fun setName(user: User, text: String) {
        this.textView.text = text
        if (user.isProsperity()) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(user), null)
    }

    fun setName(user: UserItem) {
        this.textView.text = user.fullName
        if (user.isProsperity()) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(user), null)
    }

    fun setName(account: Account) {
        this.textView.text = account.fullName
        if (account.membership?.isProsperity() == true) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(account), null)
    }

    fun setName(user: ParticipantItem) {
        this.textView.text = user.fullName
        if (user.isProsperity()) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(user), null)
    }

    fun setName(user: CallUser) {
        this.textView.text = user.fullName
        if (user.membership?.isProsperity() == true) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(user), null)
    }

    fun setName(item: ChatMinimal) {
        this.textView.text = item.fullName
        if (item.isProsperity()) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(item), null)
    }

    fun setName(item: ConversationMinimal) {
        if (item.isGroupConversation()) {
            this.textView.text = item.groupName
            iconView.isVisible = false
            iconView.stopAnimation()
            this.textView.setCompoundDrawables(null, null, null, null)
        } else {
            this.textView.text = item.name
            if (item.isProsperity()) {
                iconView.isVisible = true
                iconView.setImageDrawable(
                    RLottieDrawable(
                        R.raw.prosperity,
                        "prosperity",
                        badgeSize,
                        badgeSize,
                    ).apply {
                        setAllowDecodeSingleFrame(true)
                        setAutoRepeat(1)
                        setAutoRepeatCount(Int.MAX_VALUE)
                        start()
                    },
                )
            } else {
                iconView.isVisible = false
                iconView.stopAnimation()
            }
            this.textView.setCompoundDrawables(null, null, getBadge(item), null)
        }
    }

    fun setName(message: MessageItem) {
        this.textView.text = message.sharedUserFullName
        if (message.sharedMembership?.isProsperity() == true) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(message), null)
    }

    fun setName(message: SearchMessageDetailItem) {
        this.textView.text = message.userFullName
        if (message.isProsperity()) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(message), null)
    }

    fun setName(message: ChatHistoryMessageItem) {
        this.textView.text = message.sharedUserFullName
        if (message.isSharedProsperity()) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(message), null)
    }

    fun setName(conversationItem: ConversationItem) {
        this.textView.text = conversationItem.getConversationName()
        if (conversationItem.isProsperity()) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(conversationItem), null)
    }

    fun setName(message: SearchMessageItem) {
        this.textView.text = if (message.conversationName.isNullOrEmpty()) {
            message.userFullName
        } else {
            message.conversationName
        }
        if (message.isProsperity()) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(message), null)
    }

    fun setName(app: ExploreApp) {
        this.textView.text = app.name
        if (app.isProsperity()) {
            iconView.isVisible = true
            iconView.setImageDrawable(
                RLottieDrawable(
                    R.raw.prosperity,
                    "prosperity",
                    badgeSize,
                    badgeSize,
                ).apply {
                    setAllowDecodeSingleFrame(true)
                    setAutoRepeat(1)
                    setAutoRepeatCount(Int.MAX_VALUE)
                    start()
                },
            )
        } else {
            iconView.isVisible = false
            iconView.stopAnimation()
        }
        this.textView.setCompoundDrawables(null, null, getBadge(app), null)
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

    private fun getBadge(user: UserItem): Drawable? {
        val resources = if (user.isMembership()) {
            user.membership.membershipIcon()
        } else if (user.isVerified == true) {
            R.drawable.ic_user_verified
        } else if (user.appId != null) {
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

    private fun getBadge(account: Account): Drawable? {
        val resources = if (account.membership?.isMembership() == true) {
            account.membership.membershipIcon()
        } else {
            null
        }
        return resources?.let { res ->
            AppCompatResources.getDrawable(context, res).also { icon ->
                icon?.setBounds(0, 0, badgeSize, badgeSize)
            }
        }
    }

    private fun getBadge(user: ParticipantItem): Drawable? {
        val resources = if (user.isMembership()) {
            user.membership.membershipIcon()
        } else if (user.isVerified == true) {
            R.drawable.ic_user_verified
        } else if (!user.appId.isNullOrEmpty()) {
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
        } else if (item.isVerified()) {
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

    private fun getBadge(user: CallUser): Drawable? {
        val resources = if (user.membership?.isMembership() == true) {
            user.membership.membershipIcon()
        } else if (user.isVerified == true) {
            R.drawable.ic_user_verified
        } else if (!user.appId.isNullOrEmpty()) {
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

    private fun getBadge(item: SearchMessageDetailItem): Drawable? {
        val resources = if (item.membership?.isMembership() == true) {
            item.membership.membershipIcon()
        } else if (item.isVerified) {
            R.drawable.ic_user_verified
        } else if (!item.appId.isNullOrEmpty()) {
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

    private fun getBadge(item: ChatMinimal): Drawable? {
        val resources = if (item.isMembership()) {
            item.membership.membershipIcon()
        } else if (item.checkIsVerified()) {
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

    private fun getBadge(item: ConversationItem): Drawable? {
        val resources = if (item.isMembership()) {
            item.membership.membershipIcon()
        } else if (item.isVerified()) {
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

    private fun getBadge(message: SearchMessageItem): Drawable? {
        val resources = if (message.isMembership()) {
            message.membership.membershipIcon()
        } else if (message.isVerified()) {
            R.drawable.ic_user_verified
        } else if (message.isBot()) {
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

    private fun getBadge(app: ExploreApp): Drawable? {
        val resources = if (app.isMembership()) {
            app.membership.membershipIcon()
        } else if (app.isVerified == true) {
            R.drawable.ic_user_verified
        } else {
            R.drawable.ic_bot
        }
        return resources?.let { res ->
            AppCompatResources.getDrawable(context, res).also { icon ->
                icon?.setBounds(0, 0, badgeSize, badgeSize)
            }
        }
    }

    fun highLight(filter: String?) {
        this.textView.highLight(filter)
    }
}