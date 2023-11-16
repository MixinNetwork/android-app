package one.mixin.android.util

import one.mixin.android.vo.assetIdToAsset
import org.junit.Test
import kotlin.test.assertEquals

class AssetTest {
    @Test
    fun testUUID() {
        val asset = assetIdToAsset("31d2ea9c-95eb-3355-b65b-ba096853bc18")
        println(asset)
        assertEquals("2d08123eaeb70546c9981d9650da07e63758c5334e097b244f1a4267eb9851fb", asset)
    }
}
