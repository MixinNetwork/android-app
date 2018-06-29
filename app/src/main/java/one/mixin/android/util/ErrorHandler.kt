package one.mixin.android.util

import android.content.Context
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
                        is SocketTimeoutException -> ctx.toast(R.string.error_connection_timeout)
                        is UnknownHostException -> ctx.toast(R.string.error_no_connection)
                        is ServerErrorException -> ctx.toast(R.string.error_server_5xx)
                        is ClientErrorException -> {
                            handleErrorCode(throwable.code, ctx)
                        }
                        is NetworkException -> ctx.toast(R.string.error_no_connection)
                        else -> ctx.toast(R.string.error_unknown)
                    }
                    else -> ctx.toast(R.string.error_unknown)
                }
            }
        }

        fun handleMixinError(code: Int) {
            val ctx = MixinApplication.appContext
            handleErrorCode(code, ctx)
            ctx.runOnUiThread {
                when (code) {
                    TRANSACTION -> {
                    }
                    BAD_DATA -> {
                        toast(R.string.error_bad_data)
                    }
                    PHONE_SMS_DELIVERY -> {
                        toast(R.string.error_phone_sms_delivery)
                    }
                    RECAPTCHA_IS_INVALID -> {
                        toast(R.string.error_recaptcha_is_invalid)
                    }
                    OLD_VERSION -> {
                        toast(R.string.error_old_version)
                    }
                    PHONE_INVALID_FORMAT -> {
                        toast(R.string.error_phone_invalid_format)
                    }
                    INSUFFICIENT_IDENTITY_NUMBER -> {
                    }
                    INVALID_INVITATION_CODE -> {
                    }
                    PHONE_VERIFICATION_CODE_INVALID -> {
                        toast(R.string.error_phone_verification_code_invalid)
                    }
                    PHONE_VERIFICATION_CODE_EXPIRED -> {
                        toast(R.string.error_phone_verification_code_expired)
                    }
                    INVALID_QR_CODE -> {
                    }
                    NOT_FOUND -> {
                        toast(R.string.error_not_found)
                    }
                    GROUP_CHAT_FULL -> {
                        toast(R.string.error_full_group)
                    }
                    INSUFFICIENT_BALANCE -> {
                        toast(R.string.error_insufficient_balance)
                    }
                    INVALID_PIN_FORMAT -> {
                        toast(R.string.error_invalid_pin_format)
                    }
                    PIN_INCORRECT -> {
                        toast(R.string.error_pin_incorrect)
                    }
                    TOO_SMALL -> {
                        toast(R.string.error_too_small)
                    }
                    TOO_MANY_REQUEST -> {
                        toast(R.string.error_too_many_request)
                    }
                    USED_PHONE -> {
                        toast(R.string.error_used_phone)
                    }
                    INSUFFICIENT_TRANSACTION_FEE -> {
                        toast(R.string.error_insufficient_transaction_fee)
                    }
                    TOO_MANY_STICKERS -> {
                        toast(R.string.error_too_many_stickers)
                    }
                    BLOCKCHAIN_ERROR -> {
                        toast(R.string.error_blockchain)
                    }
                    INVALID_ADDRESS -> {
                        toast(R.string.error_invalid_address)
                    }
                }
            }
        }

        private fun handleErrorCode(code: Int, ctx: Context) {
            ctx.runOnUiThread {
                when (code) {
                    BAD_REQUEST -> {
                    }
                    AUTHENTICATION -> {
                        ctx.toast(R.string.error_authentication)
                        MixinApplication.get().closeAndClear()
                    }
                    FORBIDDEN -> {
                        ctx.toast(R.string.error_forbidden)
                    }
                    NOT_FOUND -> {
                        ctx.toast(R.string.error_not_found)
                    }
                    TOO_MANY_REQUEST -> {
                        ctx.toast(R.string.error_too_many_request)
                    }
                    SERVER -> {
                        ctx.toast(R.string.error_server_5xx)
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

        private const val TRANSACTION = 10001
        private const val BAD_DATA = 10002
        private const val PHONE_SMS_DELIVERY = 10003
        private const val RECAPTCHA_IS_INVALID = 10004
        const val NEED_RECAPTCHA = 10005
        private const val OLD_VERSION = 10006
        private const val PHONE_INVALID_FORMAT = 20110
        private const val INSUFFICIENT_IDENTITY_NUMBER = 20111
        private const val INVALID_INVITATION_CODE = 20112
        private const val PHONE_VERIFICATION_CODE_INVALID = 20113
        private const val PHONE_VERIFICATION_CODE_EXPIRED = 20114
        private const val INVALID_QR_CODE = 20115
        private const val GROUP_CHAT_FULL = 20116
        private const val INSUFFICIENT_BALANCE = 20117
        private const val INVALID_PIN_FORMAT = 20118
        const val PIN_INCORRECT = 20119
        private const val TOO_SMALL = 20120
        private const val USED_PHONE = 20122
        private const val INSUFFICIENT_TRANSACTION_FEE = 20124
        private const val TOO_MANY_STICKERS = 20126
        private const val BLOCKCHAIN_ERROR = 30100
        private const val INVALID_ADDRESS = 30102
    }
}