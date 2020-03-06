package one.mixin.android.vo

import androidx.room.TypeConverters
import java.util.ArrayList

@TypeConverters(ArrayConverters::class)
class AppItem(
    val appId: String,
    val appNumber: String,
    val homeUri: String,
    val redirectUri: String,
    val name: String,
    val iconUrl: String,
    val description: String,
    val appSecret: String,
    val capabilities: ArrayList<String>?,
    val creatorId: String,
    val resourcePatterns: ArrayList<String>?,
    val updatedAt: String? = null,
    val userId: String? = null,
    val avatarUrl: String? = null
)

fun AppItem.toApp() = App(appId, appNumber, homeUri, redirectUri, name, iconUrl, description,
    appSecret, capabilities, creatorId, resourcePatterns, updatedAt)
