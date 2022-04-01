package one.mixin.android.crypto.storage

import android.content.Context
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.db.SessionDao
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.crypto.vo.Session
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SessionStore
import timber.log.Timber
import java.io.IOException

class MixinSessionStore(context: Context) : SessionStore {

    private val sessionDao: SessionDao = SignalDatabase.getDatabase(context).sessionDao()

    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        synchronized(FILE_LOCK) {
            val session = sessionDao.getSession(address.name, address.deviceId)
            if (session != null) {
                try {
                    return SessionRecord(session.record)
                } catch (e: IOException) {
                    Timber.tag(TAG).w("No existing session information found.")
                }
            }
            return SessionRecord()
        }
    }

    override fun getSubDeviceSessions(name: String): List<Int> {
        synchronized(FILE_LOCK) {
            return sessionDao.getSubDevice(name)
        }
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        synchronized(FILE_LOCK) {
            val session = sessionDao.getSession(address.name, address.deviceId)
            if (session == null) {
                sessionDao.insert(Session(address.name, address.deviceId, record.serialize(), System.currentTimeMillis()))
                return
            }
            if (!session.record.contentEquals(record.serialize())) {
                sessionDao.insert(Session(address.name, address.deviceId, record.serialize(), System.currentTimeMillis()))
            }
        }
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        synchronized(FILE_LOCK) {
            sessionDao.getSession(address.name, address.deviceId) ?: return false
            val sessionRecord = loadSession(address)

            return sessionRecord.sessionState.hasSenderChain() && sessionRecord.sessionState.sessionVersion == CiphertextMessage.CURRENT_VERSION
        }
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        synchronized(FILE_LOCK) {
            val session = sessionDao.getSession(address.name, address.deviceId)
            if (session != null) {
                sessionDao.delete(session)
            }
        }
    }

    override fun deleteAllSessions(name: String) {
        synchronized(FILE_LOCK) {
            val devices = getSubDeviceSessions(name)

            deleteSession(SignalProtocolAddress(name, SignalProtocol.DEFAULT_DEVICE_ID))

            for (device in devices) {
                deleteSession(SignalProtocolAddress(name, device))
            }
        }
    }

    fun archiveSiblingSessions(address: SignalProtocolAddress) {
        synchronized(FILE_LOCK) {
            val sessions = sessionDao.getSessions(address.name)
            try {
                for (row in sessions) {
                    if (row.device != address.deviceId) {
                        val record = SessionRecord(row.record)
                        record.archiveCurrentState()
                        storeSession(SignalProtocolAddress(row.address, row.device), record)
                    }
                }
            } catch (e: IOException) {
                Timber.tag(TAG).w("archiveSiblingSessions new SessionRecord failed")
            }
        }
    }

    companion object {

        private val TAG = MixinSessionStore::class.java.simpleName
        private val FILE_LOCK = Any()
    }
}
