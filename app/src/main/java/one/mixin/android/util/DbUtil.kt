package one.mixin.android.util

import android.content.Context
import one.mixin.android.Constants
import one.mixin.android.session.Session
import java.io.File

fun getDbPath(context: Context, name: String): String {
    val identityNumber = requireNotNull(Session.getAccount()?.identityNumber)
    val toDir = File(context.getDatabasePath(Constants.DataBase.DB_NAME).parentFile, identityNumber)
    if (!toDir.exists()) toDir.mkdirs()
    return File(toDir, name).absolutePath
}
