package one.mixin.android.extension;

fun isNullOrEmpty(byteArray:ByteArray?): Boolean {
    return byteArray == null || byteArray.isEmpty()
}