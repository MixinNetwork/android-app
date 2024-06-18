package one.mixin.android.vo.safe

import androidx.room.ColumnInfo

class SafeCollection(
    @ColumnInfo(name = "collection_hash")
    val collectionHash: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon_url")
    val iconURL: String,
    @ColumnInfo("inscription_count")
    val inscriptionCount: Int,
)
