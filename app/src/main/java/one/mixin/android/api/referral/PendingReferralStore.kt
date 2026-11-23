package one.mixin.android.api.referral

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.util.Locale

object PendingReferralStore {
    const val ONELINK_HOST = "mixin.onelink.me"

    private const val PREFS_NAME = "pending_referral_prefs"
    private const val KEY_CODE = "pending_referral_code"
    private const val KEY_SAVED_AT = "pending_referral_saved_at"
    private const val EXPIRY_MILLIS = 7L * 24 * 60 * 60 * 1000
    private const val DEEP_LINK_VALUE_REFERRAL = "referral"
    private const val DEEP_LINK_SUB1 = "deep_link_sub1"
    private const val PATH_REFERRALS = "referrals"
    private const val MIXIN_HOST = "mixin.one"

    private val _pendingCodeFlow = MutableStateFlow<String?>(null)
    val pendingCodeFlow: StateFlow<String?> = _pendingCodeFlow.asStateFlow()

    fun deepLinkSub1Key(): String = DEEP_LINK_SUB1

    fun save(
        context: Context,
        rawCode: String?,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        val code = normalizeCode(rawCode) ?: return false
        prefs(context).edit {
            putString(KEY_CODE, code)
            putLong(KEY_SAVED_AT, nowMs)
        }
        _pendingCodeFlow.value = code
        return true
    }

    fun get(
        context: Context,
        nowMs: Long = System.currentTimeMillis(),
    ): String? {
        val sharedPrefs = prefs(context)
        val code = normalizeCode(sharedPrefs.getString(KEY_CODE, null))
        if (code == null) {
            if (_pendingCodeFlow.value != null) {
                _pendingCodeFlow.value = null
            }
            return null
        }
        val savedAt = sharedPrefs.getLong(KEY_SAVED_AT, 0L)
        if (savedAt <= 0L || nowMs - savedAt > EXPIRY_MILLIS) {
            clear(context)
            return null
        }
        if (_pendingCodeFlow.value != code) {
            _pendingCodeFlow.value = code
        }
        return code
    }

    fun clear(context: Context) {
        prefs(context).edit {
            clear()
        }
        _pendingCodeFlow.value = null
    }

    fun extractCodeFromDeepLink(
        deepLinkValue: String?,
        deepLinkSub1: String?,
    ): String? {
        val value = deepLinkValue?.trim().orEmpty()
        if (value.equals(DEEP_LINK_VALUE_REFERRAL, ignoreCase = true)) {
            return normalizeCode(deepLinkSub1)
        }
        return extractCodeFromUrl(value)
    }

    fun extractCodeFromUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
        val host = uri.host?.lowercase(Locale.US) ?: return null
        val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }

        return when {
            (scheme == "https" || scheme == "http") && host == MIXIN_HOST -> {
                if (segments.size >= 2 && segments[0].equals(PATH_REFERRALS, ignoreCase = true)) {
                    normalizeCode(segments[1])
                } else {
                    null
                }
            }
            scheme == "mixin" && host == PATH_REFERRALS -> {
                normalizeCode(segments.firstOrNull())
            }
            scheme == "mixin" && host == MIXIN_HOST -> {
                if (segments.size >= 2 && segments[0].equals(PATH_REFERRALS, ignoreCase = true)) {
                    normalizeCode(segments[1])
                } else {
                    null
                }
            }
            else -> null
        }
    }

    fun isOneLinkUrl(rawUrl: String?): Boolean {
        val trimmed = rawUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(ONELINK_HOST, ignoreCase = true)
    }

    fun normalizeCode(raw: String?): String? =
        raw?.trim()?.uppercase(Locale.US)?.takeIf { it.isNotEmpty() }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
