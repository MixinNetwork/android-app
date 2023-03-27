package one.mixin.android.tip.wc

import android.content.SharedPreferences
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import java.util.Date

data class WCV1Session(
    val session: WCSession,
    val chainId: Int,
    val remotePeerMeta: WCPeerMeta,
    val peerId: String,
    val remotePeerId: String?,
    val address: String?,
    val date: Date = Date(),
)

class WCV1SessionStore(
    private val sharedPreferences: SharedPreferences,
    builder: GsonBuilder = GsonBuilder(),
) {
    private val gson = builder
        .serializeNulls()
        .create()

    fun store(item: WCV1Session) {
        val json = sharedPreferences.getString(walletConnectV1Sessions, null)
        if (json == null) {
            sharedPreferences.edit().putString(walletConnectV1Sessions, gson.toJson(listOf(item))).apply()
        } else {
            val sessions: MutableList<WCV1Session> = gson.fromJson<List<WCV1Session>>(json).toMutableList()
            sessions.removeAll { s ->
                s.remotePeerMeta.url == item.remotePeerMeta.url
            }
            sessions.add(item)
            sharedPreferences.edit().putString(walletConnectV1Sessions, gson.toJson(sessions)).apply()
        }
    }

    fun load(): List<WCV1Session>? {
        val json = sharedPreferences.getString(walletConnectV1Sessions, null) ?: return null
        return gson.fromJson(json)
    }

    fun removeByTopic(topic: String) {
        val json = sharedPreferences.getString(walletConnectV1Sessions, null) ?: return

        val sessions: MutableList<WCV1Session> = gson.fromJson<List<WCV1Session>>(json).toMutableList()
        sessions.removeIf { s -> s.session.topic == topic }
        sharedPreferences.edit().putString(walletConnectV1Sessions, gson.toJson(sessions)).apply()
    }

    companion object {
        private const val walletConnectV1Sessions = "wallet.connect.v1.sessions"
    }
}
