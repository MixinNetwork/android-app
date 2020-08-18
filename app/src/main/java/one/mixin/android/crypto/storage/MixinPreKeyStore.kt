package one.mixin.android.crypto.storage

import android.content.Context
import android.util.Log
import one.mixin.android.crypto.db.PreKeyDao
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.crypto.db.SignedPreKeyDao
import one.mixin.android.crypto.vo.PreKey
import one.mixin.android.crypto.vo.SignedPreKey
import org.whispersystems.libsignal.InvalidKeyIdException
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.PreKeyStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyStore
import java.io.IOException
import java.util.LinkedList
import kotlin.jvm.Throws

class MixinPreKeyStore(context: Context) : PreKeyStore, SignedPreKeyStore {

    private val prekeyDao: PreKeyDao
    private val signedPreKeyDao: SignedPreKeyDao

    init {
        val database = SignalDatabase.getDatabase(context)
        prekeyDao = database.preKeyDao()
        signedPreKeyDao = database.signedPreKeyDao()
    }

    @Throws(InvalidKeyIdException::class)
    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        synchronized(FILE_LOCK) {
            try {
                val preKey = prekeyDao.getPreKey(preKeyId) ?: throw InvalidKeyIdException("No Pre Key: $preKeyId")
                return PreKeyRecord(preKey.record)
            } catch (e: IOException) {
                Log.w(TAG, e)
                throw InvalidKeyIdException(e)
            }
        }
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        synchronized(FILE_LOCK) {
            prekeyDao.insert(PreKey(preKeyId, record.serialize()))
        }
    }

    fun storePreKeyList(preKeyList: List<PreKey>) {
        synchronized(FILE_LOCK) {
            prekeyDao.insertList(preKeyList)
        }
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        val preKey = prekeyDao.getPreKey(preKeyId)
        return preKey != null
    }

    override fun removePreKey(preKeyId: Int) {
        prekeyDao.delete(preKeyId)
    }

    @Throws(InvalidKeyIdException::class)
    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        synchronized(FILE_LOCK) {
            try {
                val signedPreKey = signedPreKeyDao.getSignedPreKey(signedPreKeyId)
                if (signedPreKey != null) {
                    return SignedPreKeyRecord(signedPreKey.record)
                }
                throw InvalidKeyIdException("No such signed prekey: $signedPreKeyId")
            } catch (e: IOException) {
                Log.w(TAG, e)
                throw InvalidKeyIdException(e)
            }
        }
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        synchronized(FILE_LOCK) {
            val signedPreKeyList = signedPreKeyDao.getSignedPreKeyList()
            val results = LinkedList<SignedPreKeyRecord>()

            try {
                for (signedPreKey in signedPreKeyList) {
                    results.add(SignedPreKeyRecord(signedPreKey.record))
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return results
        }
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        synchronized(FILE_LOCK) {
            signedPreKeyDao.insert(SignedPreKey(signedPreKeyId, record.serialize(), System.currentTimeMillis()))
        }
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        val signedPreKey = signedPreKeyDao.getSignedPreKey(signedPreKeyId)
        return signedPreKey != null
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        signedPreKeyDao.delete(signedPreKeyId)
    }

    companion object {
        private val FILE_LOCK = Any()
        private val TAG = MixinPreKeyStore::class.java.simpleName
    }
}
