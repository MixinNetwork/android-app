package one.mixin.android.util

import androidx.core.app.NotificationManagerCompat
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.INTERVAL_24_HOURS
import one.mixin.android.Constants.INTERVAL_48_HOURS
import one.mixin.android.Constants.INTERVAL_7_DAYS
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openNotificationSetting
import one.mixin.android.extension.putLong
import one.mixin.android.session.Session
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.vo.Account
import one.mixin.android.widget.BulletinView

class BulletinBoard {
    private val bulletins = mutableListOf<Bulletin>()

    fun addBulletin(bulletin: Bulletin) =
        apply {
            bulletins += bulletin
        }

    fun post(): Boolean {
        val chain = Bulletin.Chain(bulletins, 0)
        return chain.proceed()
    }

    fun clear() {
        bulletins.clear()
    }
}

fun interface Bulletin {
    fun show(chain: Chain): Boolean

    class Chain(
        private val bulletins: List<Bulletin>,
        private val index: Int,
    ) {
        fun proceed(): Boolean {
            if (index >= bulletins.size) return false

            val bulletin = bulletins[index]
            val next = Chain(bulletins, index + 1)
            return bulletin.show(next)
        }
    }
}

class NewVersionBulletin(
    private val bulletinView: BulletinView,
    private val account: Account?,
    private val activity: MainActivity,
    private val onClose: (BulletinView.Type) -> Unit,
) : Bulletin {
    companion object {
        const val PREF_NEW_VERSION = "pref_new_version"
    }

    private val context = bulletinView.context

    override fun show(chain: Bulletin.Chain): Boolean {
        val appVersion = account?.system?.messenger
        if (appVersion != null) {
            val newVersionTime = context.defaultSharedPreferences.getLong(PREF_NEW_VERSION, 0)
            if (System.currentTimeMillis() - newVersionTime > INTERVAL_24_HOURS && compareVersions(appVersion.version.replace("v", "")) > 0) {
                bulletinView.setTypeAndCallback(BulletinView.Type.NewVersion, bulletinNewVersionCallback)
                return true
            }
        }
        return chain.proceed()
    }

    private fun compareVersions(remoteVersion: String): Int {
        val remoteVersionParts = remoteVersion.split(".")
        val localVersionParts = BuildConfig.VERSION_NAME.split(".")

        val maxLength = maxOf(remoteVersionParts.size, localVersionParts.size)

        for (i in 0 until maxLength) {
            val v1 = remoteVersionParts.getOrNull(i)?.toIntOrNull() ?: 0
            val v2 = localVersionParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (v1 != v2) {
                return v1.compareTo(v2)
            }
        }
        return 0
    }

    private val bulletinNewVersionCallback =
        object : BulletinView.Callback {
            override fun onClose() {
                context.defaultSharedPreferences.putLong(
                    PREF_NEW_VERSION,
                    System.currentTimeMillis(),
                )
                onClose(BulletinView.Type.NewVersion)
            }

            override fun onSetting() {
                activity.showUpdate(account?.system?.messenger?.releaseUrl)
            }
        }
}

class NewWalletBulletin(
    private val bulletinView: BulletinView,
    private val activity: MainActivity,
    private val onClose: (BulletinView.Type) -> Unit,
) : Bulletin {
    companion object {
        const val PREF_NEW_WALLET = "pref_new_wallet"
    }

    private val context = bulletinView.context

    override fun show(chain: Bulletin.Chain): Boolean {
        val newWalletTime = context.defaultSharedPreferences.getLong(PREF_NEW_WALLET, 0)
        if (System.currentTimeMillis() - newWalletTime > INTERVAL_24_HOURS &&
            (Session.getAccount()?.hasPin == true).not()
        ) {
            bulletinView.setTypeAndCallback(BulletinView.Type.NewWallet, bulletinNewWalletCallback)
            return true
        }
        return chain.proceed()
    }

    private val bulletinNewWalletCallback =
        object : BulletinView.Callback {
            override fun onClose() {
                context.defaultSharedPreferences.putLong(
                    PREF_NEW_WALLET,
                    System.currentTimeMillis(),
                )
                onClose(BulletinView.Type.NewWallet)
            }

            override fun onSetting() {
                activity.openWallet()
            }
        }
}

class NotificationBulletin(
    private val bulletinView: BulletinView,
    private val onClose: (BulletinView.Type) -> Unit,
) : Bulletin {
    companion object {
        private const val PREF_NOTIFICATION_ON = "pref_notification_on"
    }

    private val context = bulletinView.context

    override fun show(chain: Bulletin.Chain): Boolean {
        val notificationTime = context.defaultSharedPreferences.getLong(PREF_NOTIFICATION_ON, 0)
        if (System.currentTimeMillis() - notificationTime > INTERVAL_48_HOURS &&
            NotificationManagerCompat.from(context).areNotificationsEnabled().not()
        ) {
            bulletinView.setTypeAndCallback(BulletinView.Type.Notification, bulletinNotificationCallback)
            return true
        }
        return chain.proceed()
    }

    private val bulletinNotificationCallback =
        object : BulletinView.Callback {
            override fun onClose() {
                context.defaultSharedPreferences.putLong(
                    PREF_NOTIFICATION_ON,
                    System.currentTimeMillis(),
                )
                onClose(BulletinView.Type.Notification)
            }

            override fun onSetting() {
                context.openNotificationSetting()
            }
        }
}

class EmergencyContactBulletin(
    private val bulletinView: BulletinView,
    private val extraCondition: Boolean,
    private val onClose: (BulletinView.Type) -> Unit,
) : Bulletin {
    companion object {
        private const val PREF_EMERGENCY_CONTACT = "pref_emergency_contact"
    }

    private val context = bulletinView.context

    override fun show(chain: Bulletin.Chain): Boolean {
        val emergencyContactTime = context.defaultSharedPreferences.getLong(PREF_EMERGENCY_CONTACT, 0)
        if (System.currentTimeMillis() - emergencyContactTime > INTERVAL_7_DAYS &&
            extraCondition && Session.hasPhone() &&
            (Session.getAccount()?.hasEmergencyContact == true).not()
        ) {
            bulletinView.setTypeAndCallback(BulletinView.Type.EmergencyContact, bulletinEmergencyContactCallback)
            return true
        }
        return chain.proceed()
    }

    private val bulletinEmergencyContactCallback =
        object : BulletinView.Callback {
            override fun onClose() {
                context.defaultSharedPreferences.putLong(
                    PREF_EMERGENCY_CONTACT,
                    System.currentTimeMillis(),
                )
                onClose(BulletinView.Type.EmergencyContact)
            }

            override fun onSetting() {
                SettingActivity.showEmergencyContact(context)
            }
        }
}

class BackupMnemonicPhraseBulletin(
    private val bulletinView: BulletinView,
    private val account: Account?,
) : Bulletin {

    private val context = bulletinView.context

    override fun show(chain: Bulletin.Chain): Boolean {
        val display = !Session.saltExported() && Session.isAnonymous()
        if (display) {
            bulletinView.setTypeAndCallback(BulletinView.Type.BackupMnemonicPhrase, bulletinMnemonicPhraseCallback)
        }
        return display
    }

    private val bulletinMnemonicPhraseCallback =
        object : BulletinView.Callback {
            override fun onClose() {
               // do nothing
            }


            override fun onSetting() {
                SettingActivity.showMnemonicPhrase(context)
            }
        }
}
