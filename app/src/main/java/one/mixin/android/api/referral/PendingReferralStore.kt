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
    const val ONELINK_PATH_PREFIX = "/rlR3"

    private const val PREFS_NAME = "pending_referral_prefs"
    private const val KEY_CODE = "pending_referral_code"
    private const val KEY_SIGN_UP_PENDING = "pending_referral_sign_up_pending"
    private const val DEEP_LINK_VALUE_REFERRAL = "referral"
    private const val DEEP_LINK_SUB1 = "deep_link_sub1"
    private const val PATH_REFERRALS = "referrals"
    private const val MIXIN_HOST = "mixin.one"

    private val _pendingCodeFlow = MutableStateFlow<String?>(null)
    val pendingCodeFlow: StateFlow<String?> = _pendingCodeFlow.asStateFlow()

    // True from the first home session after sign-up until the process ends.
    private var postSignUpSessionActive = false

    fun deepLinkSub1Key(): String = DEEP_LINK_SUB1

    fun save(context: Context, rawCode: String?): Boolean {
        val code = normalizeCode(rawCode) ?: return false
        prefs(context).edit {
            putString(KEY_CODE, code)
        }
        _pendingCodeFlow.value = code
        return true
    }

    fun markSignUpCompleted(context: Context) {
        prefs(context).edit(commit = true) {
            putBoolean(KEY_SIGN_UP_PENDING, true)
        }
    }

    fun getForPostSignUp(context: Context): String? {
        val sharedPrefs = prefs(context)
        if (sharedPrefs.getBoolean(KEY_SIGN_UP_PENDING, false)) {
            sharedPrefs.edit { remove(KEY_SIGN_UP_PENDING) }
            postSignUpSessionActive = true
        }
        if (!postSignUpSessionActive) return null
        return get(context)
    }

    internal fun endPostSignUpSessionForTest() {
        postSignUpSessionActive = false
    }

    fun get(context: Context): String? {
        val code = normalizeCode(prefs(context).getString(KEY_CODE, null))
        if (_pendingCodeFlow.value != code) {
            _pendingCodeFlow.value = code
        }
        return code
    }

    fun clear(context: Context) {
        prefs(context).edit {
            clear()
        }
        postSignUpSessionActive = false
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
        val path = uri.path.orEmpty()
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(ONELINK_HOST, ignoreCase = true) &&
            (path == ONELINK_PATH_PREFIX || path.startsWith("$ONELINK_PATH_PREFIX/"))
    }

    fun normalizeCode(raw: String?): String? =
        raw?.trim()?.uppercase(Locale.US)?.takeIf { it.isNotEmpty() }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
