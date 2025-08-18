package one.mixin.android.db.property

import one.mixin.android.MixinApplication
import one.mixin.android.db.WalletDatabase
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.Property

object Web3PropertyHelper {

    suspend fun updateKeyValue(
        key: String,
        value: String,
    ) {
        val propertyDao = WalletDatabase.getDatabase(MixinApplication.appContext).web3PropertyDao()
        propertyDao.insertSuspend(Property(key, value, nowInUtc()))
    }

    suspend fun updateKeyValue(
        key: String,
        value: Long,
    ) {
        updateKeyValue(key, value.toString())
    }

    suspend fun updateKeyValue(
        key: String,
        value: Int,
    ) {
        updateKeyValue(key, value.toString())
    }

    suspend fun updateKeyValue(
        key: String,
        value: Boolean,
    ) {
        updateKeyValue(key, value.toString())
    }

    suspend fun deleteKeyValue(key: String) {
        val propertyDao = WalletDatabase.getDatabase(MixinApplication.appContext).web3PropertyDao()
        propertyDao.deletePropertyByKey(key)
    }

    suspend fun <T> findValueByKey(
        key: String,
        default: T,
    ): T {
        val propertyDao = WalletDatabase.getDatabase(MixinApplication.appContext).web3PropertyDao()
        val value = propertyDao.findValueByKey(key) ?: return default
        return try {
            when (default) {
                is Int -> {
                    value.toIntOrNull() ?: default
                }
                is Long -> {
                    value.toIntOrNull() ?: default
                }
                is Boolean -> {
                    value.toBooleanStrictOrNull() ?: default
                }
                else -> {
                    value
                }
            } as T
        } catch (e: Exception) {
            default
        }
    }

}
