package one.mixin.android.extension

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import okio.ByteString
import one.mixin.android.api.SignalKey
import one.mixin.android.api.response.SignalKeyCount
import one.mixin.android.webrtc.PeerList
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageData

private val gson by lazy { Gson() }

fun JsonElement.toSignalKeys() = gson.fromJson<ArrayList<SignalKey>>(this)

fun JsonElement.toBlazeMessageData(): BlazeMessageData = gson.fromJson(this, BlazeMessageData::class.java)

fun JsonElement.toPeerList(): PeerList = gson.fromJson(this, PeerList::class.java)

fun JsonElement.toSignalKeyCount(): SignalKeyCount = gson.fromJson(this, SignalKeyCount::class.java)

fun ByteString.toBlazeMessage(): BlazeMessage = gson.fromJson(this.ungzip(), BlazeMessage::class.java)

inline fun <reified T> Gson.fromJson(json: JsonElement) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)!!

fun BlazeMessage.toJson(): ByteString = gson.toJson(this).gzip()
