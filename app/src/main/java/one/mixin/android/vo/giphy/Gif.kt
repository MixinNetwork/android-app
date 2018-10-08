package one.mixin.android.vo.giphy

import androidx.recyclerview.widget.DiffUtil
import com.google.gson.annotations.SerializedName

class Gif(
    @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("images")
    val images: ImageSet
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Gif>() {
            override fun areItemsTheSame(p0: Gif, p1: Gif): Boolean {
                return p0.id == p1.id
            }

            override fun areContentsTheSame(p0: Gif, p1: Gif): Boolean {
                return p0 == p1
            }
        }
    }
}