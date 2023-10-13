package one.mixin.android.extension

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeTest {

    @Test
    fun fileSizeTest() {
        assertEquals(1024L.fileSize(), "1.00 KB")
        assertEquals(1024L.fileSize(FileSizeUnit.KB), "1.00 MB")
        assertEquals(1126L.fileSize(FileSizeUnit.MB), "1.10 GB")
        assertEquals(2147483648L.fileSize(), "2.00 GB")
        assertEquals(2621440L.fileSize(FileSizeUnit.KB), "2.50 GB")
        assertEquals(2048L.fileSize(FileSizeUnit.MB), "2.00 GB")
        assertEquals(2L.fileSize(FileSizeUnit.GB), "2.00 GB")

        assertEquals(1024L.fileUnit(), FileSizeUnit.KB.name)
        assertEquals(1024L.fileUnit(FileSizeUnit.KB), FileSizeUnit.MB.name)
        assertEquals(1126L.fileUnit(FileSizeUnit.MB), FileSizeUnit.GB.name)
        assertEquals(2147483648L.fileUnit(), FileSizeUnit.GB.name)
        assertEquals(2621440L.fileUnit(FileSizeUnit.KB), FileSizeUnit.GB.name)
        assertEquals(2048L.fileUnit(FileSizeUnit.MB), FileSizeUnit.GB.name)
        assertEquals(2L.fileUnit(FileSizeUnit.GB), FileSizeUnit.GB.name)
    }
}
