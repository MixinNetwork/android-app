package one.mixin.android.util

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.ClientErrorException
import one.mixin.android.api.DataErrorException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ServerErrorException
import one.mixin.android.extension.runOnUiThread
import one.mixin.android.extension.toast
import one.mixin.android.tip.exception.TipNodeException
import one.mixin.android.tip.getTipExceptionMsg
import org.chromium.net.CronetException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException

open class ErrorHandler {
    companion object {
        fun handleError(throwable: Throwable) {
            val ctx = MixinApplication.appContext
            ctx.runOnUiThread {
                when (throwable) {
                    is HttpException -> {
                        handleErrorCode(throwable.code(), ctx)
                    }
                    is IOException ->
                        when (throwable) {
                            is SocketTimeoutException -> toast(R.string.error_connection_timeout)
                            is UnknownHostException -> toast(R.string.No_network_connection)
                            is ServerErrorException -> toast(getString(R.string.error_server_5xx_code, throwable.code))
                            is ClientErrorException -> {
                                handleErrorCode(throwable.code, ctx)
                            }
                            is NetworkException -> toast(R.string.No_network_connection)
                            is DataErrorException -> toast(R.string.Data_error)
                            is CronetException -> {
                                handleCronetException(throwable)
                            }
                            else -> toast(getString(R.string.error_unknown_with_message, throwable.message))
                        }
                    is CancellationException -> {
                        // ignore kotlin coroutine job cancellation exception
                    }
                    is TipNodeException -> {
                        toast(throwable.getTipExceptionMsg(ctx))
                    }
                    is ExecutionException -> {
                        if (throwable.cause is CronetException) {
                            handleCronetException(throwable.cause as CronetException)
                        } else {
                            toast(R.string.error_connection_error)
                        }
                    }
                    else -> toast(getString(R.string.error_unknown_with_message, throwable.message))
                }
            }
        }

        fun handleMixinError(
            code: Int,
            message: String,
            extraMgs: String? = null,
        ) {
            val ctx = MixinApplication.appContext
            ctx.runOnUiThread {
                val extra =
                    if (!extraMgs.isNullOrBlank()) {
                        "$extraMgs\n"
                    } else {
                        ""
                    }
                toast("$extra${getMixinErrorStringByCode(code, message)}")
            }
        }

        private fun handleErrorCode(
            code: Int,
            ctx: Context,
        ) {
            ctx.runOnUiThread {
                when (code) {
                    BAD_REQUEST -> {
                    }
                    AUTHENTICATION -> {
                        toast(getString(R.string.error_authentication))
                        reportException(IllegalStateException("Force logout error code."))
                    }
                    FORBIDDEN -> {
                        toast(R.string.Access_denied)
                    }
                    NOT_FOUND -> {
                        toast(getString(R.string.error_not_found))
                    }
                    TOO_MANY_REQUEST -> {
                        toast(getString(R.string.error_too_many_request))
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

        private fun handleCronetException(e: CronetException) {
            val ctx = MixinApplication.appContext
            val extra =
                if (e is org.chromium.net.NetworkException) {
                    "${e.errorCode}, ${e.cronetInternalErrorCode}"
                } else {
                    ""
                }
            toast("${ctx.getString(R.string.error_connection_error)} $extra")
        }

        val errorHandler =
            CoroutineExceptionHandler { _, error ->
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
        const val ADDRESS_GENERATING = 10104
        const val INVALID_UTXO = 10106
        const val USER_NOT_FOUND = 10404
        const val EXPIRED_CARD = 10601
        const val EXPIRED_PRICE = 10602
        const val CAPTURE_FAILED = 10603
        const val UNSUPPORTED_CARD = 10604
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
        const val EXPIRED_AUTHORIZATION_CODE = 20121
        const val USED_PHONE = 20122
        const val INSUFFICIENT_TRANSACTION_FEE = 20124
        const val TRANSFER_IS_ALREADY_PAID = 20125
        const val TOO_MANY_STICKERS = 20126
        const val WITHDRAWAL_AMOUNT_SMALL = 20127
        const val TOO_MANY_FRIENDS = 20128
        const val INVALID_CODE_TOO_FREQUENT = 20129
        const val INVALID_EMERGENCY_CONTACT = 20130
        const val WITHDRAWAL_MEMO_FORMAT_INCORRECT = 20131
        const val FAVORITE_LIMIT = 20132
        const val CIRCLE_LIMIT = 20133
        const val WITHDRAWAL_FEE_TOO_SMALL = 20135
        const val WITHDRAWAL_SUSPEND = 20137
        const val CONVERSATION_CHECKSUM_INVALID_ERROR = 20140
        const val BLOCKCHAIN_ERROR = 30100
        const val INVALID_ADDRESS = 30102
        const val INSUFFICIENT_POOL = 30103
    }
}

fun Context.getMixinErrorStringByCode(
    code: Int,
    message: String,
): String {
    return when (code) {
        ErrorHandler.TRANSACTION -> "${ErrorHandler.TRANSACTION} TRANSACTION"
        ErrorHandler.BAD_DATA -> {
            getString(R.string.error_bad_data)
        }
        ErrorHandler.PHONE_SMS_DELIVERY -> {
            getString(R.string.error_phone_sms_delivery)
        }
        ErrorHandler.RECAPTCHA_IS_INVALID -> {
            getString(R.string.error_recaptcha_is_invalid)
        }
        ErrorHandler.OLD_VERSION -> {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            getString(R.string.error_old_version, versionName)
        }
        ErrorHandler.USER_NOT_FOUND -> {
            getString(R.string.error_opponent_not_registered_to_safe)
        }
        ErrorHandler.EXPIRED_CARD -> {
            getString(R.string.error_expired_card)
        }
        ErrorHandler.EXPIRED_PRICE -> {
            getString(R.string.error_expired_price)
        }
        ErrorHandler.CAPTURE_FAILED -> {
            getString(R.string.error_payment_capture)
        }
        ErrorHandler.UNSUPPORTED_CARD -> {
            getString(R.string.error_not_support_card)
        }
        ErrorHandler.PHONE_INVALID_FORMAT -> {
            getString(R.string.error_phone_invalid_format)
        }
        ErrorHandler.INSUFFICIENT_IDENTITY_NUMBER -> "${ErrorHandler.INSUFFICIENT_IDENTITY_NUMBER} INSUFFICIENT_IDENTITY_NUMBER"
        ErrorHandler.INVALID_INVITATION_CODE -> "${ErrorHandler.INVALID_INVITATION_CODE} INVALID_INVITATION_CODE"
        ErrorHandler.PHONE_VERIFICATION_CODE_INVALID -> {
            getString(R.string.error_phone_verification_code_invalid)
        }
        ErrorHandler.PHONE_VERIFICATION_CODE_EXPIRED -> {
            getString(R.string.error_phone_verification_code_expired)
        }
        ErrorHandler.INVALID_QR_CODE -> "${ErrorHandler.INVALID_QR_CODE} ${getString(R.string.Invalid_QR_Code)}"
        ErrorHandler.NOT_FOUND -> {
            getString(R.string.error_not_found)
        }
        ErrorHandler.GROUP_CHAT_FULL -> {
            getString(R.string.error_full_group)
        }
        ErrorHandler.INSUFFICIENT_BALANCE -> {
            getString(R.string.error_insufficient_balance)
        }
        ErrorHandler.INVALID_PIN_FORMAT -> {
            getString(R.string.error_invalid_pin_format)
        }
        ErrorHandler.PIN_INCORRECT -> {
            getString(R.string.error_pin_incorrect)
        }
        ErrorHandler.TOO_SMALL -> {
            getString(R.string.error_too_small_transfer_amount)
        }
        ErrorHandler.EXPIRED_AUTHORIZATION_CODE -> {
            getString(R.string.error_expired_authorization_code)
        }
        ErrorHandler.TOO_MANY_REQUEST -> {
            getString(R.string.error_too_many_request)
        }
        ErrorHandler.USED_PHONE -> {
            getString(R.string.error_used_phone)
        }
        ErrorHandler.TRANSFER_IS_ALREADY_PAID -> {
            getString(R.string.error_transfer_is_already_paid)
        }
        ErrorHandler.TOO_MANY_STICKERS -> {
            getString(R.string.error_too_many_stickers)
        }
        ErrorHandler.BLOCKCHAIN_ERROR -> {
            getString(R.string.error_blockchain)
        }
        ErrorHandler.INVALID_ADDRESS -> {
            getString(R.string.error_invalid_address_plain)
        }
        ErrorHandler.WITHDRAWAL_AMOUNT_SMALL -> {
            getString(R.string.error_too_small_withdraw_amount)
        }
        ErrorHandler.INVALID_UTXO -> {
            getString(R.string.error_invalid_utxo)
        }
        ErrorHandler.TOO_MANY_FRIENDS -> {
            getString(R.string.error_too_many_friends)
        }
        ErrorHandler.INVALID_CODE_TOO_FREQUENT -> {
            getString(R.string.error_invalid_code_too_frequent)
        }
        ErrorHandler.INVALID_EMERGENCY_CONTACT -> {
            getString(R.string.error_invalid_emergency_contact)
        }
        ErrorHandler.WITHDRAWAL_MEMO_FORMAT_INCORRECT -> {
            getString(R.string.error_withdrawal_memo_format_incorrect)
        }
        ErrorHandler.WITHDRAWAL_SUSPEND -> {
            getString(R.string.error_withdrawal_suspend)
        }
        ErrorHandler.FAVORITE_LIMIT, ErrorHandler.CIRCLE_LIMIT -> {
            getString(R.string.error_number_reached_limit)
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
