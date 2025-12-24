package one.mixin.android.ui.wallet.transfer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemTransferReceiveContentBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.dp
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.vo.User

class TransferContentReceiveItem : LinearLayout {
    private val _binding: ItemTransferReceiveContentBinding
    private val dp28 = 28.dp
    private val dp8 = 8.dp
    private val dp6 = 6.dp

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ItemTransferReceiveContentBinding.inflate(LayoutInflater.from(context), this)
        setPadding(dp28, dp8, dp28, dp8)
    }

    @SuppressLint("SetTextI18n")
    fun setContent(
        @StringRes titleRes: Int,
        user: User,
        userClick: (User) -> Unit,
    ) {
        _binding.apply {
            title.text = context.getString(titleRes).uppercase()
            userContainer.removeAllViews()
            userContainer.isVisible = true
            privacyContainer.isVisible = false
            val item = TransferReceiverItem(context)
            item.setContent(user)
            item.setOnClickListener {
                userClick(user)
            }
            userContainer.addView(item, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp6
                bottomMargin = dp6
            })
        }
    }

    @SuppressLint("SetTextI18n")
    fun setContent(
        @StringRes titleRes: Int,
        label: String,
        @DrawableRes iconRes: Int,
        role: String?,
    ) {
        _binding.apply {
            title.text = context.getString(titleRes).uppercase()
            userContainer.isVisible = false
            privacyContainer.isVisible = true
            privacyTv.text = label
            val drawable = ContextCompat.getDrawable(context, iconRes)
            drawable?.setBounds(0, 0, 22.dp, 22.dp)
            privacyTv.compoundDrawablePadding = 4.dp
            privacyTv.setCompoundDrawablesRelative(null, null, drawable, null)
            if (role.isNullOrBlank()) {
                roleTv.isVisible = false
            } else {
                roleTv.isVisible = true
                roleTv.setBackgroundResource(R.drawable.bg_round_4_solid_light_gray)
                roleTv.setTextColor(roleTv.context.colorAttr(R.attr.text_remarks))
                roleTv.setText(
                    if (role.equalsIgnoreCase("owner")) {
                        R.string.Wallet_Owner
                    } else {
                        R.string.Wallet_Member
                    }
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun setContent(
        @StringRes titleRes: Int,
        privacy: Boolean = true
    ) {
        _binding.apply {
            title.text = context.getString(titleRes).uppercase()
            userContainer.isVisible = false
            privacyContainer.isVisible = true
            roleTv.isVisible = false
            privacyTv.setText(R.string.Privacy_Wallet)
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_wallet_privacy)
            drawable?.setBounds(0, 0, 22.dp, 22.dp)
            privacyTv.compoundDrawablePadding = 4.dp
            privacyTv.setCompoundDrawablesRelative(null, null, drawable, null)
        }
    }

    @SuppressLint("SetTextI18n")
    fun setContent(
        @StringRes titleRes: Int,
        label: String,
        @DrawableRes iconRes: Int,
        isWalletOwner: Boolean? = null,
    ) {
        _binding.apply {
            title.text = context.getString(titleRes).uppercase()
            userContainer.isVisible = false
            privacyContainer.isVisible = true
            privacyTv.text = label
            val drawable = ContextCompat.getDrawable(context, iconRes)
            drawable?.setBounds(0, 0, 22.dp, 22.dp)
            privacyTv.compoundDrawablePadding = 4.dp
            privacyTv.setCompoundDrawablesRelative(null, null, drawable, null)
            if (isWalletOwner != null) {
                roleTv.isVisible = true
                if (isWalletOwner) {
                    roleTv.setBackgroundResource(R.drawable.bg_round_4_solid_orange)
                    roleTv.setTextColor(Color.WHITE)
                } else {
                    roleTv.setBackgroundResource(R.drawable.bg_round_4_solid_light_gray)
                    roleTv.setTextColor(roleTv.context.colorAttr(R.attr.text_remarks))
                }
                roleTv.setText(if (isWalletOwner) R.string.Wallet_Owner else R.string.Wallet_Member)
            } else {
                roleTv.isVisible = false
            }
        }
    }



    @SuppressLint("SetTextI18n")
    fun setContent(
        @PluralsRes titleRes: Int,
        users: List<User>,
        threshold: Int? = null,
        userClick: (User) -> Unit,
        signers: List<String>? = null
    ) {
        _binding.apply {
            if (threshold == 1 && users.size == 1) {
                title.text = context.resources.getQuantityString(titleRes, users.size).uppercase()
            } else if (threshold != null && users.size > 1) {
                title.text = "${
                    context.resources.getQuantityString(titleRes, users.size).uppercase()
                } ($threshold/${users.size})"
            } else {
                title.text = context.resources.getQuantityString(titleRes, users.size).uppercase()
            }
            userContainer.removeAllViews()
            userContainer.isVisible = true
            privacyContainer.isVisible = false
            users.forEach { user ->
                val item = TransferReceiverItem(context)
                item.setContent(user, signers)
                item.setOnClickListener {
                    userClick(user)
                }
                userContainer.addView(item, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp6
                    bottomMargin = dp6
                })
            }
        }
    }
}
