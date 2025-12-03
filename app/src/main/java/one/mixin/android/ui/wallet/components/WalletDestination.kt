package one.mixin.android.ui.wallet.components

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import timber.log.Timber

sealed class WalletDestination {
    object Privacy : WalletDestination()
    data class Classic(val walletId: String) : WalletDestination()
    data class Import(val walletId: String, val category: String) : WalletDestination()
    data class Watch(val walletId: String, val category: String) : WalletDestination()
    data class Safe(val walletId: String, val isOwner: Boolean, val chainId: String?, val url: String?) : WalletDestination()
}


class WalletDestinationTypeAdapter : TypeAdapter<WalletDestination>() {

    override fun write(out: JsonWriter, value: WalletDestination?) {
        Timber.e("WalletDestinationTypeAdapter.write called with: $value")
        if (value == null) {
            out.nullValue()
            return
        }

        out.beginObject()
        when (value) {
            is WalletDestination.Privacy -> {
                out.name("type").value("Privacy")
            }

            is WalletDestination.Classic -> {
                out.name("type").value("Classic")
                out.name("walletId").value(value.walletId)
            }

            is WalletDestination.Import -> {
                out.name("type").value("Import")
                out.name("walletId").value(value.walletId)
                out.name("category").value(value.category)
            }

            is WalletDestination.Watch -> {
                out.name("type").value("Watch")
                out.name("walletId").value(value.walletId)
                out.name("category").value(value.category)
            }

            is WalletDestination.Safe -> {
                out.name("type").value("Safe")
                out.name("walletId").value(value.walletId)
                out.name("isOwner").value(value.isOwner)
                out.name("chainId").value(value.chainId)
                value.url?.let { out.name("url").value(it) }
            }
        }
        out.endObject()
        Timber.e("WalletDestinationTypeAdapter.write completed")
    }

    override fun read(input: JsonReader): WalletDestination? {
        if (input.peek() == JsonToken.NULL) {
            input.nextNull()
            return null
        }

        input.beginObject()
        var type: String? = null
        var walletId: String? = null
        var category: String? = null
        var isOwner: Boolean = false
        var chainId: String? = null
        var url: String? = null

        while (input.hasNext()) {
            when (input.nextName()) {
                "type" -> type = input.nextString()
                "walletId" -> walletId = input.nextString()
                "category" -> category = input.nextString()
                "isOwner" -> isOwner = input.nextBoolean()
                "chainId" -> chainId = input.nextString()
                "url" -> url = input.nextString()
                else -> input.skipValue()
            }
        }
        input.endObject()

        return when (type) {
            "Privacy" -> WalletDestination.Privacy
            "Classic" -> WalletDestination.Classic(walletId ?: "")
            "Import" -> WalletDestination.Import(walletId ?: "", category ?: "")
            "Watch" -> WalletDestination.Watch(walletId ?: "", category ?: "")
            "Safe" -> WalletDestination.Safe(walletId ?: "", isOwner, chainId, url)
            else -> throw JsonParseException("Unknown type: $type")
        }
    }
}