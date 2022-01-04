package one.mixin.android.moshi.adapter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.Util
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import java.lang.reflect.InvocationTargetException

class IceCandidateJsonAdapter(
    moshi: Moshi
) : JsonAdapter<IceCandidate>() {
    private val options: JsonReader.Options =
        JsonReader.Options.of("sdpMid", "sdpMLineIndex", "sdp", "serverUrl", "adapterType")

    private val stringAdapter: JsonAdapter<String> = moshi.adapter(
        String::class.java, emptySet(),
        "sdpMid"
    )

    private val nullableStringAdapter: JsonAdapter<String?> = moshi.adapter(
        String::class.java,
        emptySet(), "serverUrl"
    )

    private val intAdapter: JsonAdapter<Int> = moshi.adapter(
        Int::class.java, emptySet(),
        "sdpMLineIndex"
    )

    override fun toString(): String = buildString(34) {
        append("GeneratedJsonAdapter(").append("IceCandidate").append(')')
    }

    override fun fromJson(reader: JsonReader): IceCandidate {
        var sdpMid: String? = null
        var sdpMLineIndex: Int? = null
        var sdp: String? = null
        var serverUrl: String? = null
        var adapterType: PeerConnection.AdapterType = PeerConnection.AdapterType.UNKNOWN
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> sdpMid = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "sdpMid",
                    "sdpMid", reader
                )
                1 -> sdpMLineIndex = intAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "sdpMLineIndex",
                    "sdpMLineIndex",
                    reader
                )
                2 -> sdp = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "sdp",
                    "sdp",
                    reader
                )
                3 -> serverUrl = nullableStringAdapter.fromJson(reader)
                4 -> {
                    val type = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                        "adapterType",
                        "adapterType",
                        reader
                    )
                    adapterType = when (type) {
                        PeerConnection.AdapterType.ETHERNET.name ->
                            PeerConnection.AdapterType
                                .ETHERNET
                        PeerConnection.AdapterType.WIFI.name -> PeerConnection.AdapterType.WIFI
                        PeerConnection.AdapterType.CELLULAR.name ->
                            PeerConnection.AdapterType
                                .CELLULAR
                        PeerConnection.AdapterType.VPN.name -> PeerConnection.AdapterType.VPN
                        PeerConnection.AdapterType.LOOPBACK.name ->
                            PeerConnection.AdapterType
                                .LOOPBACK
                        PeerConnection.AdapterType.ADAPTER_TYPE_ANY.name ->
                            PeerConnection
                                .AdapterType.ADAPTER_TYPE_ANY
                        PeerConnection.AdapterType.CELLULAR_2G.name ->
                            PeerConnection.AdapterType
                                .CELLULAR_2G
                        PeerConnection.AdapterType.CELLULAR_3G.name ->
                            PeerConnection.AdapterType
                                .CELLULAR_3G
                        PeerConnection.AdapterType.CELLULAR_4G.name ->
                            PeerConnection.AdapterType
                                .CELLULAR_4G
                        PeerConnection.AdapterType.CELLULAR_5G.name ->
                            PeerConnection.AdapterType
                                .CELLULAR_5G
                        else -> PeerConnection.AdapterType.UNKNOWN
                    }
                }
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        return constructorIceCandidate(
            sdpMid ?: throw Util.missingProperty("sdpMid", "sdpMid", reader),
            sdpMLineIndex ?: throw Util.missingProperty(
                "sdpMLineIndex",
                "sdpMLineIndex", reader
            ),
            sdp ?: throw Util.missingProperty("sdp", "sdp", reader), serverUrl, adapterType
        )
    }

    @Throws(
        NoSuchMethodException::class,
        IllegalAccessException::class,
        InvocationTargetException::class,
        InstantiationException::class
    )
    private fun constructorIceCandidate(
        sdpMid: String?,
        sdpMLineIndex: Int,
        sdp: String?,
        serverUrl: String?,
        adapterType: PeerConnection.AdapterType?
    ): IceCandidate {
        val constructor = IceCandidate::class.java.getDeclaredConstructor(
            String::class.java,
            Int::class.javaPrimitiveType,
            String::class.java,
            String::class.java,
            PeerConnection.AdapterType::class.java
        )
        constructor.isAccessible = true
        return constructor.newInstance(sdpMid, sdpMLineIndex, sdp, serverUrl, adapterType)
    }

    override fun toJson(writer: JsonWriter, value_: IceCandidate?) {
        if (value_ == null) {
            throw NullPointerException(
                "value_ was null! Wrap in .nullSafe() to write nullable " +
                    "values."
            )
        }
        writer.beginObject()
        writer.name("sdpMid")
        stringAdapter.toJson(writer, value_.sdpMid)
        writer.name("sdpMLineIndex")
        intAdapter.toJson(writer, value_.sdpMLineIndex)
        writer.name("sdp")
        stringAdapter.toJson(writer, value_.sdp)
        writer.name("adapterType")
        when (value_.adapterType) {
            PeerConnection.AdapterType.ETHERNET -> writer.value(
                PeerConnection.AdapterType
                    .ETHERNET.name
            )
            PeerConnection.AdapterType.WIFI -> writer.value(PeerConnection.AdapterType.WIFI.name)
            PeerConnection.AdapterType.CELLULAR -> writer.value(
                PeerConnection.AdapterType
                    .CELLULAR.name
            )
            PeerConnection.AdapterType.VPN -> writer.value(PeerConnection.AdapterType.VPN.name)
            PeerConnection.AdapterType.LOOPBACK -> writer.value(
                PeerConnection.AdapterType
                    .LOOPBACK.name
            )
            PeerConnection.AdapterType.ADAPTER_TYPE_ANY -> writer.value(
                PeerConnection
                    .AdapterType.ADAPTER_TYPE_ANY.name
            )
            PeerConnection.AdapterType.CELLULAR_2G -> writer.value(
                PeerConnection.AdapterType
                    .CELLULAR_2G.name
            )
            PeerConnection.AdapterType.CELLULAR_3G -> writer.value(
                PeerConnection.AdapterType
                    .CELLULAR_3G.name
            )
            PeerConnection.AdapterType.CELLULAR_4G -> writer.value(
                PeerConnection.AdapterType
                    .CELLULAR_4G.name
            )
            PeerConnection.AdapterType.CELLULAR_5G -> writer.value(
                PeerConnection.AdapterType
                    .CELLULAR_5G.name
            )
            else -> writer.value(PeerConnection.AdapterType.UNKNOWN.name)
        }
        writer.endObject()
    }
}
