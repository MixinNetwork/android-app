package one.mixin.android.util

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

class HiddenAnnotationExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipClass(clazz: Class<*>): Boolean {
        return clazz.getAnnotation(JsonSkip::class.java) != null
    }

    override fun shouldSkipField(f: FieldAttributes): Boolean {
        return f.getAnnotation(JsonSkip::class.java) != null
    }
}
