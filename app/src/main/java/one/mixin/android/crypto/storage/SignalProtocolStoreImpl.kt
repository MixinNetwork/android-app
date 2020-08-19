package one.mixin.android.crypto.storage

import android.content.Context
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.InvalidKeyIdException
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.state.IdentityKeyStore
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.PreKeyStore
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SessionStore
import org.whispersystems.libsignal.state.SignalProtocolStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyStore
import kotlin.jvm.Throws

class SignalProtocolStoreImpl(context: Context) : SignalProtocolStore {

    private val preKeyStore: PreKeyStore
    private val signedPreKeyStore: SignedPreKeyStore
    private val identityKeyStore: MixinIdentityKeyStore
    private val sessionStore: SessionStore

    init {
        this.preKeyStore = MixinPreKeyStore(context)
        this.signedPreKeyStore = MixinPreKeyStore(context)
        this.identityKeyStore = MixinIdentityKeyStore(context)
        this.sessionStore = MixinSessionStore(context)
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        return identityKeyStore.getIdentity(address)
    }

    override fun getIdentityKeyPair(): IdentityKeyPair {
        return identityKeyStore.identityKeyPair
    }

    override fun getLocalRegistrationId(): Int {
        return identityKeyStore.localRegistrationId
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        return identityKeyStore.saveIdentity(address, identityKey)
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        return identityKeyStore.isTrustedIdentity(address, identityKey, direction)
    }

    @Throws(InvalidKeyIdException::class)
    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        return preKeyStore.loadPreKey(preKeyId)
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        preKeyStore.storePreKey(preKeyId, record)
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return preKeyStore.containsPreKey(preKeyId)
    }

    override fun removePreKey(preKeyId: Int) {
        preKeyStore.removePreKey(preKeyId)
    }

    override fun loadSession(axolotlAddress: SignalProtocolAddress): SessionRecord {
        return sessionStore.loadSession(axolotlAddress)
    }

    override fun getSubDeviceSessions(number: String): List<Int> {
        return sessionStore.getSubDeviceSessions(number)
    }

    override fun storeSession(axolotlAddress: SignalProtocolAddress, record: SessionRecord) {
        sessionStore.storeSession(axolotlAddress, record)
    }

    override fun containsSession(axolotlAddress: SignalProtocolAddress): Boolean {
        return sessionStore.containsSession(axolotlAddress)
    }

    override fun deleteSession(axolotlAddress: SignalProtocolAddress) {
        sessionStore.deleteSession(axolotlAddress)
    }

    override fun deleteAllSessions(number: String) {
        sessionStore.deleteAllSessions(number)
    }

    @Throws(InvalidKeyIdException::class)
    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        return signedPreKeyStore.loadSignedPreKey(signedPreKeyId)
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        return signedPreKeyStore.loadSignedPreKeys()
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        signedPreKeyStore.storeSignedPreKey(signedPreKeyId, record)
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return signedPreKeyStore.containsSignedPreKey(signedPreKeyId)
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        signedPreKeyStore.removeSignedPreKey(signedPreKeyId)
    }

    fun removeIdentity(address: SignalProtocolAddress) {
        identityKeyStore.removeIdentity(address)
    }
}
