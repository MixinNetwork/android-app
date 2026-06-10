package one.mixin.android.util

import one.mixin.android.vo.Chain
import org.junit.Test
import kotlin.test.assertEquals

class CollectionTest {
    @Test
    fun testSubtract() {
        val chains =
            setOf(
                Chain(
                    "6cfe566e-4aad-470b-8c9a-2fd35b49c68d",
                    "EOS",
                    "EOS",
                    "https://mixin-images.zeromesh.net/OTwqLjEwc6v0JutJc-1sYkh_juFOvVbFz26WvvwfLGdKq6ZtwAT-wKhX0k-5PsgOK_Pd9rCQjZfwMJmiNXCBzpHnjapBtkCqAVCTCg=s128",
                    64,
                    "",
                ),
                Chain(
                    "43d61dcd-e413-450d-80b8-101d5e903357",
                    "Ethereum",
                    "ETH",
                    "https://mixin-images.zeromesh.net/zVDjOxNTQvVsA8h2B4ZVxuHoCF3DJszufYKWpd9duXUSbSapoZadC7_13cnWBqg0EmwmRcKGbJaUpA8wFfpgZA=s128",
                    36,
                    "",
                ),
                Chain(
                    "c996abc9-d94e-4494-b1cf-2a3fd3ac5714",
                    "Zcash",
                    "ZEC",
                    "https://mixin-images.zeromesh.net/9QWOYgcD0H7q1cH6PaSM08FQ549epnEzqIQ2EgEfK2s82jhsIu1wDKmsR7rkPFwjIYKOILteq7mW1hIaXcy4DhI=s128",
                    32,
                    "",
                ),
            ).subtract(
                setOf(
                    Chain(
                        "43d61dcd-e413-450d-80b8-101d5e903357",
                        "Ethereum",
                        "ETH",
                        "https://mixin-images.zeromesh.net/zVDjOxNTQvVsA8h2B4ZVxuHoCF3DJszufYKWpd9duXUSbSapoZadC7_13cnWBqg0EmwmRcKGbJaUpA8wFfpgZA=s128",
                        36,
                        "",
                    ),
                    Chain(
                        "c996abc9-d94e-4494-b1cf-2a3fd3ac5714",
                        "Zcash_1", // different
                        "ZEC",
                        "https://mixin-images.zeromesh.net/9QWOYgcD0H7q1cH6PaSM08FQ549epnEzqIQ2EgEfK2s82jhsIu1wDKmsR7rkPFwjIYKOILteq7mW1hIaXcy4DhI=s128",
                        32,
                        "",
                    ),
                ),
            )
        println(chains)
        assertEquals(chains.map { it.name }.toList(), listOf("EOS", "Zcash"))
    }
}
