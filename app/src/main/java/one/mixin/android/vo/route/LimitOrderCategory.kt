package one.mixin.android.vo.route;

import com.google.gson.annotations.SerializedName;

enum class LimitOrderCategory(val value: String) {
    @SerializedName("all")
    ALL("all"),

    @SerializedName("active")
    ACTIVE("active"),

    @SerializedName("history")
    HISTORY("history");

    companion object {
        fun fromString(value: String): LimitOrderCategory {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: ALL
        }
    }
}

