package one.mixin.android.util.mention

import one.mixin.android.vo.MentionData

data class MentionRenderContext(
    val userMap: Map<String, MentionData>,
    val action: (String) -> Unit
)