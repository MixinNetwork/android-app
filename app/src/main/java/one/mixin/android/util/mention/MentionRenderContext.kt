package one.mixin.android.util.mention

import android.util.ArrayMap

data class MentionRenderContext(
    val userMap: ArrayMap<String, String>,
    val action: (String) -> Unit
)