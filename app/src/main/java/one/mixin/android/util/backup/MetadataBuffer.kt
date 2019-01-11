package one.mixin.android.util.backup

import com.google.android.gms.common.data.AbstractDataBuffer
import com.google.android.gms.common.data.DataHolder
import one.mixin.android.util.backup.drive.Metadata

class MetadataBuffer(dataHolder: DataHolder) : AbstractDataBuffer<Metadata>(dataHolder) {
    override fun get(p0: Int): Metadata {
    }
}