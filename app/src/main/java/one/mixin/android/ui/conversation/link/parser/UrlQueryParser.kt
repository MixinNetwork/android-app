package one.mixin.android.ui.conversation.link.parser

import android.net.Uri
import one.mixin.android.extension.isUUID
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.link.parser.NewSchemeParser.Companion.FAILURE
import one.mixin.android.vo.MixAddressPrefix
import one.mixin.android.vo.MixinInvoice
import one.mixin.android.vo.MixinInvoicePrefix
import one.mixin.android.vo.toMixAddress
import java.io.UnsupportedEncodingException
import java.math.BigDecimal
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class UrlQueryParser(val uri: Uri, from: Int) {
    val lastPath: String = uri.lastPathSegment ?: throw ParserError(FAILURE)

    val payType: PayType by lazy {
        if (lastPath.isUUID()) {
            PayType.Uuid
        } else if (lastPath.startsWith("XIN")) {
            PayType.XinAddress
        } else if (lastPath.startsWith(MixAddressPrefix)) {
            PayType.MixAddress
        } else if(lastPath.startsWith(MixinInvoicePrefix)) {
            PayType.Invoice
        } else {
            throw ParserError(FAILURE)
        }
    }

    val userId: String by lazy {
        if (payType == PayType.Uuid) {
            lastPath
        } else {
            throw ParserError(FAILURE)
        }
    }

    val mixAddress by lazy {
        if (payType == PayType.MixAddress) {
            lastPath.toMixAddress() ?: throw ParserError(FAILURE)
        } else {
            throw ParserError(FAILURE)
        }
    }

    val mixInvoice by lazy {
        if (payType == PayType.Invoice) {
            runCatching { MixinInvoice.fromString(lastPath) }.getOrNull() ?: throw ParserError(FAILURE)
        } else {
            throw ParserError(FAILURE)
        }
    }

    val asset: String? by lazy {
        val value = uri.getQueryParameter("asset")
        if (value != null && !value.isUUID()) throw ParserError(FAILURE)
        value
    }

    val amount: String? by lazy {
        val value = uri.getQueryParameter("amount")
        if (value != null) {
            val a = value.toBigDecimalOrNull()
            if (a == null || a <= BigDecimal.ZERO) {
                throw ParserError(FAILURE)
            }
            a.stripTrailingZeros().toPlainString()
        } else {
            null
        }
    }

    val memo: String? by lazy {
        uri.getQueryParameter("memo").run {
            Uri.decode(this)
        }
    }

    val trace: String? by lazy {
        val value = uri.getQueryParameter("trace")
        if (value != null && !value.isUUID()) throw ParserError(FAILURE)
        value
    }

    val reference: String? by lazy {
        val value = uri.getQueryParameter("reference")
        if (value != null && value.length != 64) throw ParserError(FAILURE)
        value
    }

    val returnTo: String? by lazy {
        uri.getQueryParameter("return_to")?.run {
            if (from == LinkBottomSheetDialogFragment.FROM_EXTERNAL) {
                try {
                    URLDecoder.decode(this, StandardCharsets.UTF_8.name())
                } catch (e: UnsupportedEncodingException) {
                    this
                }
            } else {
                null
            }
        }
    }

    val inscription: String? by lazy {
        uri.getQueryParameter("inscription")
    }

    val inscriptionCollection: String? by lazy {
        uri.getQueryParameter("inscription_collection")
    }
}
