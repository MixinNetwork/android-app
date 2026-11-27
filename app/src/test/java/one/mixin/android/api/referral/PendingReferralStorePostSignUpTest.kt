package one.mixin.android.api.referral

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class PendingReferralStorePostSignUpTest {
    private val context: Context = InMemoryPrefsContext()

    @Before
    fun setUp() {
        PendingReferralStore.clear(context)
    }

    @Test
    fun getForPostSignUp_requiresSignUpMarker() {
        PendingReferralStore.save(context, "crmlsv")
        assertNull(PendingReferralStore.getForPostSignUp(context))
    }

    @Test
    fun getForPostSignUp_returnsCodeOnFirstHomeSessionAfterSignUp() {
        PendingReferralStore.save(context, "crmlsv")
        PendingReferralStore.markSignUpCompleted(context)
        assertEquals("CRMLSV", PendingReferralStore.getForPostSignUp(context))
    }

    @Test
    fun getForPostSignUp_returnsLateCodeInSameSession() {
        PendingReferralStore.markSignUpCompleted(context)
        assertNull(PendingReferralStore.getForPostSignUp(context))
        PendingReferralStore.save(context, "crmlsv")
        assertEquals("CRMLSV", PendingReferralStore.getForPostSignUp(context))
    }

    @Test
    fun getForPostSignUp_doesNotReturnCodeInLaterSession() {
        PendingReferralStore.save(context, "crmlsv")
        PendingReferralStore.markSignUpCompleted(context)
        assertEquals("CRMLSV", PendingReferralStore.getForPostSignUp(context))
        PendingReferralStore.endPostSignUpSessionForTest()
        PendingReferralStore.save(context, "other1")
        assertNull(PendingReferralStore.getForPostSignUp(context))
    }

    @Test
    fun getForPostSignUp_returnsCodeFromOldPersistedRecord() {
        context.getSharedPreferences("pending_referral_prefs", Context.MODE_PRIVATE).edit()
            .putString("pending_referral_code", "CRMLSV")
            .putLong("pending_referral_saved_at", Instant.parse("2020-01-01T00:00:00Z").toEpochMilli())
            .apply()
        PendingReferralStore.markSignUpCompleted(context)
        assertEquals("CRMLSV", PendingReferralStore.getForPostSignUp(context))
        assertEquals("CRMLSV", PendingReferralStore.pendingCodeFlow.value)
    }

    @Test
    fun clear_removesPendingCodeAndSignUpMarker() {
        PendingReferralStore.save(context, "crmlsv")
        PendingReferralStore.markSignUpCompleted(context)
        PendingReferralStore.clear(context)
        assertNull(PendingReferralStore.pendingCodeFlow.value)
        assertNull(PendingReferralStore.get(context))

        PendingReferralStore.save(context, "other1")
        assertNull(PendingReferralStore.getForPostSignUp(context))
    }

    @Test
    fun clear_endsActivePostSignUpSession() {
        PendingReferralStore.save(context, "crmlsv")
        PendingReferralStore.markSignUpCompleted(context)
        assertEquals("CRMLSV", PendingReferralStore.getForPostSignUp(context))
        PendingReferralStore.clear(context)

        PendingReferralStore.save(context, "other1")
        assertNull(PendingReferralStore.getForPostSignUp(context))
    }

    private class InMemoryPrefsContext : ContextWrapper(null) {
        private val prefsByName = mutableMapOf<String, SharedPreferences>()

        override fun getApplicationContext(): Context = this

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            prefsByName.getOrPut(name) { InMemorySharedPreferences() }
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String, defValue: String?): String? =
            values[key] as? String ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
            (values[key] as? Set<String>)?.toMutableSet() ?: defValues

        override fun getInt(key: String, defValue: Int): Int =
            values[key] as? Int ?: defValue

        override fun getLong(key: String, defValue: Long): Long =
            values[key] as? Long ?: defValue

        override fun getFloat(key: String, defValue: Float): Float =
            values[key] as? Float ?: defValue

        override fun getBoolean(key: String, defValue: Boolean): Boolean =
            values[key] as? Boolean ?: defValue

        override fun contains(key: String): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = Editor()

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        private inner class Editor : SharedPreferences.Editor {
            private var cleared = false
            private val updates = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()

            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
                if (value == null) {
                    updates.remove(key)
                    removals.add(key)
                } else {
                    removals.remove(key)
                    updates[key] = value
                }
            }

            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                if (values == null) {
                    updates.remove(key)
                    removals.add(key)
                } else {
                    removals.remove(key)
                    updates[key] = values.toSet()
                }
            }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply {
                removals.remove(key)
                updates[key] = value
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
                removals.remove(key)
                updates[key] = value
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply {
                removals.remove(key)
                updates[key] = value
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply {
                removals.remove(key)
                updates[key] = value
            }

            override fun remove(key: String): SharedPreferences.Editor = apply {
                updates.remove(key)
                removals.add(key)
            }

            override fun clear(): SharedPreferences.Editor = apply {
                cleared = true
                updates.clear()
                removals.clear()
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (cleared) {
                    values.clear()
                }
                for (key in removals) {
                    values.remove(key)
                }
                values.putAll(updates)
            }
        }
    }
}
