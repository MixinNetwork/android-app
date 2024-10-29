package one.mixin.android.widget

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
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
        BackupMnemonicPhrase,
        NewVersion,
        NewWallet,
        Notification,
        EmergencyContact,
    }

    private var type: Type = Type.NewWallet

    private var callback: Callback? = null

    init {
        setBackgroundResource(R.drawable.bg_list_conversation_header)
        closeView.setOnClickListener { callback?.onClose() }
        settingView.setOnClickListener { callback?.onSetting() }
    }

    fun setTypeAndCallback(
        type: Type,
        callback: Callback,
    ) {
        this.type = type
        this.callback = callback
        when (type) {
            Type.BackupMnemonicPhrase -> {
                closeView.isVisible = false
                titleView.setText(R.string.Backup_Mnemonic_Phrase)
                contentView.setText(R.string.Backup_Mnemonic_Phrase_desc)
                settingView.setText(R.string.Backup_Now)
            }
            Type.NewVersion -> {
                closeView.isVisible = true
                titleView.setText(R.string.New_Update_Available)
                contentView.setText(R.string.New_Update_Available_desc)
                settingView.setText(R.string.Update_Now)
            }
            Type.NewWallet -> {
                closeView.isVisible = true
                titleView.setText(R.string.Get_a_new_wallet)
                contentView.setText(R.string.new_wallet_hint)
                settingView.setText(R.string.Continue)
            }
            Type.Notification -> {
                closeView.isVisible = true
                titleView.setText(R.string.Turn_On_Notifications)
                contentView.setText(R.string.notification_content)
                settingView.setText(R.string.Notifications)
            }
            else -> {
                closeView.isVisible = true
                titleView.setText(R.string.Emergency_Contact)
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
