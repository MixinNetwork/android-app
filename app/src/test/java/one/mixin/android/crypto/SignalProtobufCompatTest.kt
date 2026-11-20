package one.mixin.android.crypto

import com.google.firebase.perf.v1.PerfSession
import com.google.firebase.perf.v1.SessionVerbosity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.groups.state.SenderKeyStore
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.state.StorageProtos
import org.whispersystems.libsignal.state.impl.InMemorySignalProtocolStore

class SignalProtobufCompatTest {
    @Test
    fun signalProtocolUsesIsolatedProtobufAndRoundTripsMessages() {
        assertEquals(
            "org.whispersystems.libsignal.shaded.protobuf.GeneratedMessageLite",
            StorageProtos.IdentityKeyPairStructure::class.java.superclass?.name,
        )

        val aliceIdentityPair = Curve.generateKeyPair()
        val aliceIdentity = IdentityKeyPair(IdentityKey(aliceIdentityPair.publicKey), aliceIdentityPair.privateKey)
        val decodedIdentity = IdentityKeyPair(aliceIdentity.serialize())
        assertArrayEquals(aliceIdentity.serialize(), decodedIdentity.serialize())

        val bobIdentityPair = Curve.generateKeyPair()
        val bobIdentity = IdentityKeyPair(IdentityKey(bobIdentityPair.publicKey), bobIdentityPair.privateKey)

        val preKey = PreKeyRecord(1, Curve.generateKeyPair())
        assertEquals(1, PreKeyRecord(preKey.serialize()).id)

        val signedPreKeyPair = Curve.generateKeyPair()
        val signedPreKeySig = Curve.calculateSignature(
            bobIdentity.privateKey,
            signedPreKeyPair.publicKey.serialize(),
        )
        val signedPreKey = SignedPreKeyRecord(2, System.currentTimeMillis(), signedPreKeyPair, signedPreKeySig)
        assertEquals(2, SignedPreKeyRecord(signedPreKey.serialize()).id)

        val aliceStore = InMemorySignalProtocolStore(aliceIdentity, 100)
        val bobStore = InMemorySignalProtocolStore(bobIdentity, 200).apply {
            storePreKey(preKey.id, preKey)
            storeSignedPreKey(signedPreKey.id, signedPreKey)
        }

        val bobAddress = SignalProtocolAddress("bob", 1)
        val aliceAddress = SignalProtocolAddress("alice", 1)
        SessionBuilder(aliceStore, bobAddress).process(
            PreKeyBundle(
                200,
                1,
                preKey.id,
                preKey.keyPair.publicKey,
                signedPreKey.id,
                signedPreKey.keyPair.publicKey,
                signedPreKeySig,
                bobIdentity.publicKey,
            ),
        )

        val plaintext = "hello isolated signal protobuf".toByteArray()
        val ciphertext = SessionCipher(aliceStore, bobAddress).encrypt(plaintext)
        val decrypted = SessionCipher(bobStore, aliceAddress).decrypt(PreKeySignalMessage(ciphertext.serialize()))
        assertArrayEquals(plaintext, decrypted)

        val serializedSession = bobStore.loadSession(aliceAddress).serialize()
        assertEquals(
            bobStore.loadSession(aliceAddress).sessionState.sessionVersion,
            SessionRecord(serializedSession).sessionState.sessionVersion,
        )

        val aliceSenderKeyStore = InMemorySenderKeyStore()
        val bobSenderKeyStore = InMemorySenderKeyStore()
        val senderKeyName = SenderKeyName("group-1", aliceAddress)
        val distribution = GroupSessionBuilder(aliceSenderKeyStore).create(senderKeyName)
        GroupSessionBuilder(bobSenderKeyStore).process(
            senderKeyName,
            SenderKeyDistributionMessage(distribution.serialize()),
        )

        val groupPlaintext = "group isolated protobuf".toByteArray()
        val groupCiphertext = GroupCipher(aliceSenderKeyStore, senderKeyName).encrypt(groupPlaintext)
        val groupDecrypted = GroupCipher(bobSenderKeyStore, senderKeyName).decrypt(groupCiphertext)
        assertArrayEquals(groupPlaintext, groupDecrypted)
    }

    @Test
    fun runtimeProtobufProvidesIntConverterForFirebasePerfSession() {
        val converterClass = Class.forName("com.google.protobuf.Internal\$IntListAdapter\$IntConverter")
        assertNotNull(converterClass)

        val session = PerfSession.newBuilder()
            .setSessionId("test-session-id")
            .addSessionVerbosity(SessionVerbosity.GAUGES_AND_SYSTEM_EVENTS)
            .build()
        val parsed = PerfSession.parseFrom(session.toByteArray())
        assertTrue(parsed.sessionVerbosityList.contains(SessionVerbosity.GAUGES_AND_SYSTEM_EVENTS))
    }

    private class InMemorySenderKeyStore : SenderKeyStore {
        private val store = mutableMapOf<SenderKeyName, ByteArray>()

        override fun storeSenderKey(senderKeyName: SenderKeyName, record: SenderKeyRecord) {
            store[senderKeyName] = record.serialize()
        }

        override fun loadSenderKey(senderKeyName: SenderKeyName): SenderKeyRecord {
            val bytes = store[senderKeyName] ?: return SenderKeyRecord()
            return SenderKeyRecord(bytes)
        }
    }
}
