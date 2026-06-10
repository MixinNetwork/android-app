package one.mixin.android.util

import org.junit.Test
import kotlin.test.assertEquals

class UUIDTest {
    @Test
    fun testUUID() {
        val list =
            listOf(
                "d60e9e9c-3a3c-4c57-b8b2-53f13bb7f22c",
                "45b99ff9-de6d-4766-a456-6bae0c25d5b8",
                "1de3cae3-3ccd-4d84-afbe-a814a5f5db5e",
                "d703fc15-8fcf-4c88-8a53-c9ac0b8d2c00",
                "1e358598-4bd4-4c04-9364-4bdec4c49aa9",
                "d8df1cc3-3b50-47c0-b1b8-ad98a79a3646",
                "076a97a9-74bc-4f25-9143-b7c2bb620b68",
                "8e295a18-df44-46e9-8d42-2b75c1fbfe7d",
                "21d96d36-4064-4f4a-b083-20e92d8b71b9",
                "db67f76a-ebd6-43bf-94c0-69a5a2c9f1cb",
            )
        for ((index, uuid) in list.withIndex()) {
            val convertStr = UUIDUtils.fromByteArray(UUIDUtils.toByteArray(uuid))
            assertEquals(list[index], convertStr, "error")
        }
    }
}
