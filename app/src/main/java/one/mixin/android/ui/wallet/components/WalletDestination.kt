package one.mixin.android.ui.wallet.components
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import timber.log.Timber
import java.lang.reflect.Type
sealed class WalletDestination {
    object Privacy : WalletDestination()
    data class Classic(val walletId: String) : WalletDestination()
    data class Import(val walletId: String, val category: String) : WalletDestination()
    data class Watch(val walletId: String, val category: String) : WalletDestination()
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

        while (input.hasNext()) {
            when (input.nextName()) {
                "type" -> type = input.nextString()
                "walletId" -> walletId = input.nextString()
                "category" -> category = input.nextString()
                else -> input.skipValue()
            }
        }
        input.endObject()

        return when (type) {
            "Privacy" -> WalletDestination.Privacy
            "Classic" -> WalletDestination.Classic(walletId ?: "")
            "Import" -> WalletDestination.Import(walletId ?: "", category ?: "")
            "Watch" -> WalletDestination.Watch(walletId ?: "", category ?: "")
            else -> throw JsonParseException("Unknown type: $type")
        }
    }
}