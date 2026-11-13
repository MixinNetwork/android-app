package one.mixin.android.ui.web

internal class WebOriginPermissionGrants {
    private val grantedByOrigin = mutableMapOf<String, MutableSet<String>>()

    fun covers(
        origin: String,
        resources: Array<out String>,
    ): Boolean {
        val granted = grantedByOrigin[origin] ?: return false
        return resources.isNotEmpty() && resources.all { it in granted }
    }

    fun grant(
        origin: String,
        resources: Array<out String>,
    ) {
        if (resources.isEmpty()) return
        grantedByOrigin.getOrPut(origin) { mutableSetOf() }.addAll(resources)
    }

    fun clear() {
        grantedByOrigin.clear()
    }
}
