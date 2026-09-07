package one.mixin.android.ui.landing

import android.content.Context
import one.mixin.android.util.database.databaseFile

fun hasLocalAccountDatabase(
    context: Context,
    identityNumber: String,
): Boolean {
    val dbFile = databaseFile(context, identityNumber)
    return dbFile.exists() && dbFile.length() > 0
}
