package one.mixin.android.util

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.crashlytics.android.Crashlytics
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.ClientErrorException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ServerErrorException
import one.mixin.android.extension.toast
import org.jetbrains.anko.runOnUiThread
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
                        is UnknownHostException -> toast(R.string.error_no_connection)
                        is ServerErrorException -> toast(R.string.error_server_5xx)
                        is ClientErrorException -> {
                            handleErrorCode(throwable.code, ctx)
                        }
                        is NetworkException -> toast(R.string.error_no_connection)
                        else -> toast(R.string.error_unknown)
                    }
                    else -> toast(R.string.error_unknown)
                }
            }
        }

        fun handleMixinError(code: Int) {
            val ctx = MixinApplication.appContext
            ctx.runOnUiThread {
                var handled = true
                when (code) {
                    TRANSACTION -> {
                    }
                    BAD_DATA -> {
                        toast(getString(R.string.error_bad_data, BAD_DATA))
                    }
                    PHONE_SMS_DELIVERY -> {
                        toast(getString(R.string.error_phone_sms_delivery, PHONE_SMS_DELIVERY))
                    }
                    RECAPTCHA_IS_INVALID -> {
                        toast(getString(R.string.error_recaptcha_is_invalid, RECAPTCHA_IS_INVALID))
                    }
                    OLD_VERSION -> {
                        toast(getString(R.string.error_old_version, OLD_VERSION))
                    }
                    PHONE_INVALID_FORMAT -> {
                        toast(getString(R.string.error_phone_invalid_format, PHONE_INVALID_FORMAT))
                    }
                    INSUFFICIENT_IDENTITY_NUMBER -> {
                    }
                    INVALID_INVITATION_CODE -> {
                    }
                    PHONE_VERIFICATION_CODE_INVALID -> {
                        toast(getString(R.string.error_phone_verification_code_invalid, PHONE_VERIFICATION_CODE_INVALID))
                    }
                    PHONE_VERIFICATION_CODE_EXPIRED -> {
                        toast(getString(R.string.error_phone_verification_code_expired, PHONE_VERIFICATION_CODE_EXPIRED))
                    }
                    INVALID_QR_CODE -> {
                    }
                    NOT_FOUND -> {
                        toast(getString(R.string.error_not_found, NOT_FOUND))
                    }
                    GROUP_CHAT_FULL -> {
                        toast(getString(R.string.error_full_group, GROUP_CHAT_FULL))
                    }
                    INSUFFICIENT_BALANCE -> {
                        toast(getString(R.string.error_insufficient_balance, INSUFFICIENT_BALANCE))
                    }
                    INVALID_PIN_FORMAT -> {
                        toast(getString(R.string.error_invalid_pin_format, INVALID_PIN_FORMAT))
                    }
                    PIN_INCORRECT -> {
                        toast(getString(R.string.error_pin_incorrect, PIN_INCORRECT))
                    }
                    TOO_SMALL -> {
                        toast(getString(R.string.error_too_small, TOO_SMALL))
                    }
                    TOO_MANY_REQUEST -> {
                        toast(getString(R.string.error_too_many_request, TOO_MANY_REQUEST))
                    }
                    USED_PHONE -> {
                        toast(getString(R.string.error_used_phone, USED_PHONE))
                    }
                    INSUFFICIENT_TRANSACTION_FEE -> {
                        toast(getString(R.string.error_insufficient_transaction_fee, INSUFFICIENT_TRANSACTION_FEE))
                    }
                    TOO_MANY_STICKERS -> {
                        toast(getString(R.string.error_too_many_stickers, TOO_MANY_STICKERS))
                    }
                    BLOCKCHAIN_ERROR -> {
                        toast(getString(R.string.error_blockchain, BLOCKCHAIN_ERROR))
                    }
                    INVALID_ADDRESS -> {
                        toast(getString(R.string.error_invalid_address, INVALID_ADDRESS))
                    }
                    WITHDRAWAL_AMOUNT_SMALL -> {
                        toast(getString(R.string.error_too_small_withdraw_amount, WITHDRAWAL_AMOUNT_SMALL))
                    }
                    INVALID_EMERGENCY_CONTACT -> {
                        toast(getString(R.string.error_invalid_emergency_contact, INVALID_EMERGENCY_CONTACT))
                    }
                    else -> handled = false
                }

                if (!handled) {
                    handleErrorCode(code, ctx)
                }
            }
        }

        private fun handleErrorCode(code: Int, ctx: Context) {
            ctx.runOnUiThread {
                when (code) {
                    BAD_REQUEST -> {
                    }
                    AUTHENTICATION -> {
                        toast(getString(R.string.error_authentication, AUTHENTICATION))
                        IllegalStateException("Force logout error code.").let { exception ->
                            Bugsnag.notify(exception)
                            Crashlytics.logException(exception)
                        }
                        MixinApplication.get().closeAndClear()
                    }
                    FORBIDDEN -> {
                        toast(R.string.error_forbidden)
                    }
                    NOT_FOUND -> {
                        toast(getString(R.string.error_not_found, NOT_FOUND))
                    }
                    TOO_MANY_REQUEST -> {
                        toast(getString(R.string.error_too_many_request, TOO_MANY_REQUEST))
                    }
                    SERVER -> {
                        toast(R.string.error_server_5xx)
                    }
                    TIME_INACCURATE -> { }
                    else -> {
                        toast(getString(R.string.error_unknown_with_code, code))
                    }
                }
            }
        }

        private const val BAD_REQUEST = 400
        const val AUTHENTICATION = 401
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val TOO_MANY_REQUEST = 429
        private const val SERVER = 500
        const val TIME_INACCURATE = 911

        private const val TRANSACTION = 10001
        const val BAD_DATA = 10002
        private const val PHONE_SMS_DELIVERY = 10003
        private const val RECAPTCHA_IS_INVALID = 10004
        const val NEED_RECAPTCHA = 10005
        private const val OLD_VERSION = 10006
        private const val PHONE_INVALID_FORMAT = 20110
        private const val INSUFFICIENT_IDENTITY_NUMBER = 20111
        private const val INVALID_INVITATION_CODE = 20112
        const val PHONE_VERIFICATION_CODE_INVALID = 20113
        const val PHONE_VERIFICATION_CODE_EXPIRED = 20114
        private const val INVALID_QR_CODE = 20115
        private const val GROUP_CHAT_FULL = 20116
        private const val INSUFFICIENT_BALANCE = 20117
        private const val INVALID_PIN_FORMAT = 20118
        const val PIN_INCORRECT = 20119
        private const val TOO_SMALL = 20120
        private const val USED_PHONE = 20122
        private const val INSUFFICIENT_TRANSACTION_FEE = 20124
        private const val TOO_MANY_STICKERS = 20126
        private const val WITHDRAWAL_AMOUNT_SMALL = 20127
        private const val INVALID_EMERGENCY_CONTACT = 20130
        private const val BLOCKCHAIN_ERROR = 30100
        private const val INVALID_ADDRESS = 30102
    }
}