package one.mixin.android.util.mention

data class MentionRenderContext(val userMap: Map<String, String>, val action: (String) -> Unit)
