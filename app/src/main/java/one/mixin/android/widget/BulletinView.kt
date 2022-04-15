package one.mixin.android.widget

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.R
import one.mixin.android.databinding.ItemListConversationHeaderBinding

class BulletinView(context: Context) : ConstraintLayout(context) {
    private val binding: ItemListConversationHeaderBinding =
        ItemListConversationHeaderBinding.inflate(LayoutInflater.from(context), this)

    private val closeView get() = binding.headerClose
    val titleView get() = binding.headerTitle
    val contentView get() = binding.headerContent
    private val settingView get() = binding.headerSettings

    enum class Type {
        NewWallet, Notification, EmergencyContact
    }

    private var type: Type = Type.NewWallet

    private var callback: Callback? = null

    init {
        setBackgroundResource(R.drawable.bg_list_conversation_header)
        closeView.setOnClickListener { callback?.onClose() }
        settingView.setOnClickListener { callback?.onSetting() }
    }

    fun setTypeAndCallback(type: Type, callback: Callback) {
        this.type = type
        this.callback = callback
        when (type) {
            Type.NewWallet -> {
                titleView.setText(R.string.notification_new_wallet_title)
                contentView.setText(R.string.notification_new_wallet_content)
                settingView.setText(R.string.Continue)
            }
            Type.Notification -> {
                titleView.setText(R.string.notification_title)
                contentView.setText(R.string.notification_content)
                settingView.setText(R.string.Notifications)
            }
            else -> {
                titleView.setText(R.string.log_category_emergency)
                contentView.setText(R.string.setting_emergency_content)
                settingView.setText(R.string.Continue)
            }
        }
    }

    interface Callback {
        fun onClose()
        fun onSetting()
    }
}
