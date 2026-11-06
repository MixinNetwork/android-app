package one.mixin.android.util

import java.io.File

internal fun isAllowedShareUri(
    scheme: String?,
    authority: String?,
    packageName: String,
    providerPackageName: String?,
): Boolean {
    val providerAuthority = authority?.substringAfterLast('@')
    return scheme == "content" &&
        !providerAuthority.isNullOrBlank() &&
        providerAuthority != "$packageName.provider" &&
        providerPackageName != packageName
}

internal fun isPrivateAttachmentPath(file: File, dataDir: File): Boolean {
    val path = file.canonicalPath
    val privatePath = dataDir.canonicalPath
    return path == privatePath || path.startsWith(privatePath + File.separator)
}
