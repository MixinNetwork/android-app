package one.mixin.android.util.mention

import androidx.collection.ArrayMap
import androidx.collection.LruCache
import one.mixin.android.util.MoshiHelper
import one.mixin.android.vo.MentionUser

class MentionRenderCache private constructor(maxSize: Int) : LruCache<Int, Map<String, String>>(maxSize) {
    companion object {
        val singleton: MentionRenderCache by lazy { MentionRenderCache((Runtime.getRuntime().maxMemory() / 32).toInt()) }
    }

    private fun getMentionData(content: String): Map<String, String>? {
        try {
            val result = get(content.hashCode())
            return if (result != null) {
                result
            } else {
                val mentionMap = ArrayMap<String, String>()
                MoshiHelper.getTypeListAdapter<List<MentionUser>>(MentionUser::class.java)
                    .fromJson(content)?.asSequence()?.forEach { data ->
                        mentionMap[data.identityNumber] = data.fullName
                    }
                if (mentionMap.isEmpty) {
                    null
                } else {
                    put(content.hashCode(), mentionMap)
                    mentionMap
                }
            }
        } catch (e: Exception) {
            return null
        }
    }

    fun getMentionRenderContext(content: String): MentionRenderContext? {
        val mentionMap = getMentionData(content) ?: return null
        return MentionRenderContext(mentionMap)
    }
}
