package one.mixin.android.vo.safe

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
class SafeCollection(
    @ColumnInfo(name = "collection_hash")
    val collectionHash: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon_url")
    val iconURL: String,
    @ColumnInfo("inscription_count")
    val inscriptionCount: Int,
): Parcelable
