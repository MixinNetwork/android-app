package one.mixin.android.session

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.db.pending.PendingDatabaseImp
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.vo.Account
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

class MissingAccountScopeException(
    scopeName: String,
) : IllegalStateException("Account is required for $scopeName scope.")

@Singleton
class CurrentUserScopeManager
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val appContext = context.applicationContext
        private val lock = Any()

        private var currentUserId: String? = null
        private var currentIdentityNumber: String? = null
        private var scopeVersion: Long = 0L

        private var mixinDatabase: MixinDatabase? = null
        private var walletDatabase: WalletDatabase? = null
        private var pendingDatabase: PendingDatabase? = null
        private var ftsDatabase: FtsDatabase? = null

        fun enter(account: Account) {
            synchronized(lock) {
                ensureScopeLocked(account)
            }
        }

        fun ensureScopeFromSession() {
            val account = Session.getAccount() ?: return
            enter(account)
        }

        fun exit(reason: String? = null) {
            synchronized(lock) {
                if (!reason.isNullOrBlank()) {
                    Timber.d("Exit user scope: $reason")
                }
                closeScopeLocked()
            }
        }

        fun getMixinDatabase(): MixinDatabase {
            synchronized(lock) {
                val account = requireAccount("MixinDatabase")
                ensureScopeLocked(account)
                return mixinDatabase
                    ?: throw IllegalStateException("Unable to create MixinDatabase scope")
            }
        }

        fun getWalletDatabase(): WalletDatabase {
            synchronized(lock) {
                val account = requireAccount("WalletDatabase")
                ensureScopeLocked(account)
                return walletDatabase
                    ?: throw IllegalStateException("Unable to create WalletDatabase scope")
            }
        }

        fun getPendingDatabase(): PendingDatabase {
            synchronized(lock) {
                val account = requireAccount("PendingDatabase")
                ensureScopeLocked(account)
                return pendingDatabase
                    ?: throw IllegalStateException("Unable to create PendingDatabase scope")
            }
        }

        fun getFtsDatabase(): FtsDatabase {
            synchronized(lock) {
                val account = requireAccount("FtsDatabase")
                ensureScopeLocked(account)
                return ftsDatabase
                    ?: throw IllegalStateException("Unable to create FtsDatabase scope")
            }
        }

        fun getScopeVersion(): Long {
            synchronized(lock) {
                return scopeVersion
            }
        }

        private fun ensureScopeLocked(account: Account) {
            val isSameScope = currentUserId == account.userId &&
                currentIdentityNumber == account.identityNumber &&
                mixinDatabase != null &&
                walletDatabase != null &&
                pendingDatabase != null &&
                ftsDatabase != null
            if (isSameScope) {
                return
            }
            closeScopeLocked()
            MixinDatabase.migrateRelatedDatabaseFilesIfNeeded(appContext, account)
            val scopedMixinDatabase = MixinDatabase.getDatabase(appContext, account.identityNumber)
            val scopedPendingDatabase =
                PendingDatabaseImp.getDatabase(
                    appContext,
                    scopedMixinDatabase.floodMessageDao(),
                    scopedMixinDatabase.jobDao(),
                    account.identityNumber,
                )
            val scopedWalletDatabase = WalletDatabase.getDatabase(appContext, account.identityNumber)
            val scopedFtsDatabase = FtsDatabase.getDatabase(appContext, account.identityNumber)

            currentUserId = account.userId
            currentIdentityNumber = account.identityNumber
            mixinDatabase = scopedMixinDatabase
            pendingDatabase = scopedPendingDatabase
            walletDatabase = scopedWalletDatabase
            ftsDatabase = scopedFtsDatabase
            scopeVersion++
        }

        private fun closeScopeLocked() {
            (pendingDatabase as? PendingDatabaseImp)?.close()
            pendingDatabase = null
            mixinDatabase = null
            walletDatabase?.close()
            walletDatabase = null
            ftsDatabase?.close()
            ftsDatabase = null
            MixinDatabase.destroy(close = true)
            FtsDatabase.destroy(close = true)
            currentUserId = null
            currentIdentityNumber = null
            scopeVersion++
        }

        private fun requireAccount(scopeName: String): Account {
            return Session.getAccount() ?: throw MissingAccountScopeException(scopeName)
        }
    }

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CurrentUserScopeEntryPoint {
    fun getCurrentUserScopeManager(): CurrentUserScopeManager
}

fun resolveCurrentUserScopeManager(context: Context): CurrentUserScopeManager {
    val appContext = context.applicationContext
    return EntryPointAccessors.fromApplication(appContext, CurrentUserScopeEntryPoint::class.java)
        .getCurrentUserScopeManager()
}
