package one.mixin.android.util

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.ClientErrorException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ServerErrorException
import one.mixin.android.extension.runOnUiThread
import one.mixin.android.extension.toast
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

open class ErrorHandler {

    companion object {

        fun handleError(throwable: Throwable) {
            val ctx = MixinApplication.appContext
            ctx.runOnUiThread {
                when (throwable) {
                    is HttpException -> {
                        handleErrorCode(throwable.code(), ctx)
                    }
                    is IOException -> when (throwable) {
                        is SocketTimeoutException -> toast(R.string.error_connection_timeout)
                        is UnknownHostException -> toast(R.string.No_network_connection)
                        is ServerErrorException -> toast(getString(R.string.error_server_5xx_code, throwable.code))
                        is ClientErrorException -> {
                            handleErrorCode(throwable.code, ctx)
                        }
                        is NetworkException -> toast(R.string.No_network_connection)
                        else -> toast(getString(R.string.error_unknown_with_message, throwable.message))
                    }
                    is CancellationException -> {
                        // ignore kotlin coroutine job cancellation exception
                    }
                    else -> toast(getString(R.string.error_unknown_with_message, throwable.message))
                }
            }
        }

        fun handleMixinError(code: Int, message: String, extraMgs: String? = null) {
            val ctx = MixinApplication.appContext
            ctx.runOnUiThread {
                val extra = if (!extraMgs.isNullOrBlank()) {
                    "$extraMgs\n"
                } else ""
                toast("$extra${getMixinErrorStringByCode(code, message)}")
            }
        }

        private fun handleErrorCode(code: Int, ctx: Context) {
            ctx.runOnUiThread {
                when (code) {
                    BAD_REQUEST -> {
                    }
                    AUTHENTICATION -> {
                        toast(getString(R.string.error_authentication, AUTHENTICATION))
                        reportException(IllegalStateException("Force logout error code."))
                    }
                    FORBIDDEN -> {
                        toast(R.string.Access_denied)
                    }
                    NOT_FOUND -> {
                        toast(getString(R.string.error_not_found, NOT_FOUND))
                    }
                    TOO_MANY_REQUEST -> {
                        toast(getString(R.string.error_too_many_request, TOO_MANY_REQUEST))
                    }
                    SERVER, INSUFFICIENT_POOL -> {
                        toast(getString(R.string.error_server_5xx_code, code))
                    }
                    TIME_INACCURATE -> { }
                    else -> {
                        toast(getString(R.string.error_unknown_with_code, code))
                    }
                }
            }
        }

        val errorHandler = CoroutineExceptionHandler { _, error ->
            handleError(error)
        }

        private const val BAD_REQUEST = 400
        const val AUTHENTICATION = 401
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val TOO_MANY_REQUEST = 429
        const val SERVER = 500
        const val TIME_INACCURATE = 911

        const val TRANSACTION = 10001
        const val BAD_DATA = 10002
        const val PHONE_SMS_DELIVERY = 10003
        const val RECAPTCHA_IS_INVALID = 10004
        const val NEED_CAPTCHA = 10005
        const val OLD_VERSION = 10006
        const val PHONE_INVALID_FORMAT = 20110
        const val INSUFFICIENT_IDENTITY_NUMBER = 20111
        const val INVALID_INVITATION_CODE = 20112
        const val PHONE_VERIFICATION_CODE_INVALID = 20113
        const val PHONE_VERIFICATION_CODE_EXPIRED = 20114
        const val INVALID_QR_CODE = 20115
        const val GROUP_CHAT_FULL = 20116
        const val INSUFFICIENT_BALANCE = 20117
        const val INVALID_PIN_FORMAT = 20118
        const val PIN_INCORRECT = 20119
        const val TOO_SMALL = 20120
        const val USED_PHONE = 20122
        const val INSUFFICIENT_TRANSACTION_FEE = 20124
        const val TOO_MANY_STICKERS = 20126
        const val WITHDRAWAL_AMOUNT_SMALL = 20127
        const val INVALID_CODE_TOO_FREQUENT = 20129
        const val INVALID_EMERGENCY_CONTACT = 20130
        const val WITHDRAWAL_MEMO_FORMAT_INCORRECT = 20131
        const val FAVORITE_LIMIT = 20132
        const val CIRCLE_LIMIT = 20133
        const val WITHDRAWAL_FEE_TOO_SMALL = 20135
        const val CONVERSATION_CHECKSUM_INVALID_ERROR = 20140
        const val BLOCKCHAIN_ERROR = 30100
        const val INVALID_ADDRESS = 30102
        const val INSUFFICIENT_POOL = 30103
    }
}

fun Context.getMixinErrorStringByCode(code: Int, message: String): String {
    return when (code) {
        ErrorHandler.TRANSACTION -> "${ErrorHandler.TRANSACTION} TRANSACTION"
        ErrorHandler.BAD_DATA -> {
            getString(R.string.error_bad_data, ErrorHandler.BAD_DATA)
        }
        ErrorHandler.PHONE_SMS_DELIVERY -> {
            getString(R.string.error_phone_sms_delivery, ErrorHandler.PHONE_SMS_DELIVERY)
        }
        ErrorHandler.RECAPTCHA_IS_INVALID -> {
            getString(
                R.string.error_recaptcha_is_invalid,
                ErrorHandler.RECAPTCHA_IS_INVALID
            )
        }
        ErrorHandler.OLD_VERSION -> {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            getString(R.string.error_old_version, ErrorHandler.OLD_VERSION, versionName)
        }
        ErrorHandler.PHONE_INVALID_FORMAT -> {
            getString(
                R.string.error_phone_invalid_format,
                ErrorHandler.PHONE_INVALID_FORMAT
            )
        }
        ErrorHandler.INSUFFICIENT_IDENTITY_NUMBER -> "${ErrorHandler.INSUFFICIENT_IDENTITY_NUMBER} INSUFFICIENT_IDENTITY_NUMBER"
        ErrorHandler.INVALID_INVITATION_CODE -> "${ErrorHandler.INVALID_INVITATION_CODE} INVALID_INVITATION_CODE"
        ErrorHandler.PHONE_VERIFICATION_CODE_INVALID -> {
            getString(
                R.string.error_phone_verification_code_invalid,
                ErrorHandler.PHONE_VERIFICATION_CODE_INVALID
            )
        }
        ErrorHandler.PHONE_VERIFICATION_CODE_EXPIRED -> {
            getString(
                R.string.error_phone_verification_code_expired,
                ErrorHandler.PHONE_VERIFICATION_CODE_EXPIRED
            )
        }
        ErrorHandler.INVALID_QR_CODE -> "${ErrorHandler.INVALID_QR_CODE} INVALID_QR_CODE"
        ErrorHandler.NOT_FOUND -> {
            getString(R.string.error_not_found, ErrorHandler.NOT_FOUND)
        }
        ErrorHandler.GROUP_CHAT_FULL -> {
            getString(R.string.error_full_group, ErrorHandler.GROUP_CHAT_FULL)
        }
        ErrorHandler.INSUFFICIENT_BALANCE -> {
            getString(
                R.string.error_insufficient_balance,
                ErrorHandler.INSUFFICIENT_BALANCE
            )
        }
        ErrorHandler.INVALID_PIN_FORMAT -> {
            getString(R.string.error_invalid_pin_format, ErrorHandler.INVALID_PIN_FORMAT)
        }
        ErrorHandler.PIN_INCORRECT -> {
            getString(R.string.error_pin_incorrect, ErrorHandler.PIN_INCORRECT)
        }
        ErrorHandler.TOO_SMALL -> {
            getString(R.string.error_too_small, ErrorHandler.TOO_SMALL)
        }
        ErrorHandler.TOO_MANY_REQUEST -> {
            getString(R.string.error_too_many_request, ErrorHandler.TOO_MANY_REQUEST)
        }
        ErrorHandler.USED_PHONE -> {
            getString(R.string.error_used_phone, ErrorHandler.USED_PHONE)
        }
        ErrorHandler.TOO_MANY_STICKERS -> {
            getString(R.string.error_too_many_stickers, ErrorHandler.TOO_MANY_STICKERS)
        }
        ErrorHandler.BLOCKCHAIN_ERROR -> {
            getString(R.string.error_blockchain, ErrorHandler.BLOCKCHAIN_ERROR)
        }
        ErrorHandler.INVALID_ADDRESS -> {
            getString(R.string.error_invalid_address_plain, ErrorHandler.INVALID_ADDRESS)
        }
        ErrorHandler.WITHDRAWAL_AMOUNT_SMALL -> {
            getString(
                R.string.error_too_small_withdraw_amount,
                ErrorHandler.WITHDRAWAL_AMOUNT_SMALL
            )
        }
        ErrorHandler.INVALID_CODE_TOO_FREQUENT -> {
            getString(
                R.string.error_invalid_code_too_frequent,
                ErrorHandler.INVALID_CODE_TOO_FREQUENT
            )
        }
        ErrorHandler.INVALID_EMERGENCY_CONTACT -> {
            getString(
                R.string.error_invalid_emergency_contact,
                ErrorHandler.INVALID_EMERGENCY_CONTACT
            )
        }
        ErrorHandler.WITHDRAWAL_MEMO_FORMAT_INCORRECT -> {
            getString(
                R.string.error_withdrawal_memo_format_incorrect,
                ErrorHandler.WITHDRAWAL_MEMO_FORMAT_INCORRECT
            )
        }
        ErrorHandler.FAVORITE_LIMIT, ErrorHandler.CIRCLE_LIMIT -> {
            getString(
                R.string.error_favorite_limit,
                ErrorHandler.FAVORITE_LIMIT
            )
        }
        ErrorHandler.FORBIDDEN -> {
            getString(R.string.Access_denied)
        }
        ErrorHandler.SERVER, ErrorHandler.INSUFFICIENT_POOL -> {
            getString(R.string.error_server_5xx_code, code)
        }
        ErrorHandler.TIME_INACCURATE -> "${ErrorHandler.TIME_INACCURATE} TIME_INACCURATE"

        else -> {
            "${getString(R.string.error_unknown_with_code, code)}: $message"
        }
    }
}
