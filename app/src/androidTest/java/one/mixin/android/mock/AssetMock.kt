package one.mixin.android.mock

import one.mixin.android.vo.Asset

fun mockAsset() = Asset(
    "c6d0c728-2624-429b-8e0d-d9d19b6592fa", "BTC", "Bitcoin",
    "https://mixin-images.zeromesh.net/HvYGJsV5TGeZ-X9Ek3FEQohQZ3fE9LBEBGcOcn4c4BNHovP4fW4YB97Dg5LcXoQ1hUjMEgjbl1DPlKg1TW7kK6XP=s128",
    "1", "", "", "1", "10000", "c6d0c728-2624-429b-8e0d-d9d19b6592fa", "0",
    "0", 3, "", "0"
)

fun mockAssetWithDestinationAndTag() = Asset(
    "c6d0c728-2624-429b-8e0d-d9d19b6592fa", "BTC", "Bitcoin",
    "https://mixin-images.zeromesh.net/HvYGJsV5TGeZ-X9Ek3FEQohQZ3fE9LBEBGcOcn4c4BNHovP4fW4YB97Dg5LcXoQ1hUjMEgjbl1DPlKg1TW7kK6XP=s128",
    "1", "eoswithmixin", "1e40671dc72b58606a79f53e2", "1", "10000",
    "c6d0c728-2624-429b-8e0d-d9d19b6592fa", "0", "0", 3, "", "0"
)

fun mockAssetWithDestination() = Asset(
    "c6d0c728-2624-429b-8e0d-d9d19b6592fa", "BTC", "Bitcoin",
    "https://mixin-images.zeromesh.net/HvYGJsV5TGeZ-X9Ek3FEQohQZ3fE9LBEBGcOcn4c4BNHovP4fW4YB97Dg5LcXoQ1hUjMEgjbl1DPlKg1TW7kK6XP=s128",
    "1", "eoswithmixin", "", "1", "10000", "c6d0c728-2624-429b-8e0d-d9d19b6592fa", "0",
    "0", 3, "", "0"
)
