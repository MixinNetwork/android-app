package one.mixin.android.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import one.mixin.android.Constants
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.tip.privateKeyToAddress
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MnemonicAddressDerivationTest {
    private data class DerivedAddresses(
        val eth: String,
        val sol: String,
        val btc: String,
    )

    @Test
    fun derivesClassicAddressesForMnemonicIndexesZeroThroughNine() {
        val mnemonic = "blur staff nurse happy palm neutral inflict inform soup almost always canal"
        val expectedAddresses = listOf(
            DerivedAddresses("0x59076a300E56dFd7652ab561CE749AAB4E09087c", "APwPqNnVoatwPi95tA8rfBnLp6JZeyXGHmHh779zhwBH", "bc1q0l3ln6utuqrlmp2n9zy33skgyeevfvuezk9t5c"),
            DerivedAddresses("0xA4bf0a3E4A2Ee036bb905D5A5250B4E9056Ff918", "AZaxkQSnaqNm6RjnASCm7BjJc66kpJa1m95p9jHvEqnY", "bc1qhs8kfw8hj8kf90qw4elagz9dzr3xfclp6stz54"),
            DerivedAddresses("0xEa2fdc703c119141E4f3a5260b13fcECf1cD1aA7", "EFiCUQshpopmtXttsfEa2fBQBWfyUWoUuW2wjgnkbyQm", "bc1qpvzhk029gp2fe30esu98l9j6dfgtw2aalqlcqp"),
            DerivedAddresses("0xC7CBccfd6adCAfFA619A0e253F94B9C35B134888", "7rWxEkLhhcdSJg8ZD8KbLb8YVcWHVLdTwCEkzpQfa9cV", "bc1qv62cf3qty5am852wezjk2sqsah04mczfkn8x60"),
            DerivedAddresses("0xf45977F3C7902a491b49f5b21a74CDFDEA6F4E12", "EGGX4hRgiv93EYYtJZLZwfTPThnbuEBPgGzRrCpdRfF5", "bc1qh4dgf9aq4p2nzl7w4u3hgka8z4axtnktk65nf5"),
            DerivedAddresses("0xE9a92CD1EF14e7516d9F3dc0ea7a0B47eb39ae03", "CArQMDKcZDYS45KhpjAMF1kmVrisCqssb252oUGsMBQW", "bc1qpkyzkkdmrwj2vmw040kw6k2x4h4v9czvpqjd4w"),
            DerivedAddresses("0xaC5acDE406cbFdC95e8eC2c1381038d7d8d26ce9", "32SWu9NCQxqHHuzUrDRebVniav9KS8qzT8WiTaXwwLqz", "bc1qq49wpvcxyxg2tde0xpn4k3ahhvv48p5eupnq4q"),
            DerivedAddresses("0x5e31953d983E35f09f0D06E170E40D7bA966e27e", "FEMJvTfw5Ee7wm8tbRF2kFfjTzgTtnUyHyuPvvgPm3Ja", "bc1qenq8pydqq6tpjsf3dre0mv7kal05hex2wvexzk"),
            DerivedAddresses("0xeDbd4271861b35bBF31E98F1d83F3AC837501885", "4mZudB4KN8ZGexbkEm7Ao2MPcAz8BnSNEEzARA4SGwdn", "bc1q8gtw92uaapwpl00hhkr5ghvrdc7xggw2r5zgmg"),
            DerivedAddresses("0x3614E12EB2c47A7108C18AdA669FCebFBc3cA081", "FAX3U7tUDZbz7ggEe8LWYHE8udVzobHQuur5W5hQYfqF", "bc1q7mnlczmtwckrhsr9ezk04pvcqcnar7cy527hup"),
        )

        expectedAddresses.forEachIndexed { index, expected ->
            assertEquals(expected.eth, CryptoWalletHelper.mnemonicToEthereumWallet(mnemonic, index = index).address)
            assertEquals(expected.sol, CryptoWalletHelper.mnemonicToSolanaWallet(mnemonic, index = index).address)
            assertEquals(expected.btc, CryptoWalletHelper.mnemonicToBitcoinSegwitWallet(mnemonic, index = index).address)
        }
    }

    @Test
    fun derivesCommonAddressesFromTipSpendKeyIndexesZeroThroughNine() {
        val spendKey = "00e39d185e883a18949c48c834bc0b8347c6466dd0925fa084f3493d6727e0f9".hexStringToByteArray()
        val expectedAddresses = listOf(
            DerivedAddresses("0x2dbb8EBAF233b6ad93b7DFc5616A1D1971e7D264", "AutN9EZHypXoCAgi982s45pjMekzkS4jRC7UXXi9Kyub", "bc1qrwa7al55klllsqmwltgsym5jjqwujgwp34ztqa"),
            DerivedAddresses("0x1ada924ca6801447F8aca00cFD527dBbE8967871", "HaFiYPDbw4bYAthRFFURWyYUefyBgzKFR1GJiywK9fSn", "bc1q4vrhsrk0fj3hmnt55tqcsr085smvkzap462efe"),
            DerivedAddresses("0x6d81282bf22Cfe8D30205662cb5E19e6ad279fb9", "H3eQGvhWTHJLZjMPudYeSuqgzPErwL2Fk7Nn6FJNuZ47", "bc1qwquhyx7vs9ua3fxxxwgdrwglcrr34vrgewyanj"),
            DerivedAddresses("0xf2436459a303c289e9184633b8a56F6E3B62Ad02", "5vxZYUmz8CGasKXz3htAd5VZg8cscVicxqLqHRNfDV2d", "bc1qartkcn5lju05tz3jgepzqe30pnjvyged2drmqn"),
            DerivedAddresses("0xa4fb1b08f60B1Db00C319f25644580dF21de0F07", "8Qjc6hy3d3vWD9fUDipNWKaoR9g3FmgvgbCtszjAKJFz", "bc1qyhqppl6rlpg7gdcnsm8amg04srs5jck3cyhlj7"),
            DerivedAddresses("0x92cB37C836be4d86C0FEF08d09c6996Ce7d9b52A", "Fnn6JzEh555qyqZrgVF4WVVsRrFDV73zB3ucNc5cKWKf", "bc1qgez94trpc2afa9kx7722v245j0gucsed89dwaq"),
            DerivedAddresses("0xA88Ec0865A332297308AeAdd700591AcBA9dB20B", "CFYNVzxBxXePTG8HFAAyfC5uKHnCENBjFtBA7i4iBQZw", "bc1qc97rnp664jl7l970rf7xq7v4pcge0qtfdczhfp"),
            DerivedAddresses("0xaE77832c9070d526892D1a122cA420417E53BCaF", "2jFvCSFiVRFWCzUg6cDvPgGWqRmPodowpvGLNWEAtd6x", "bc1qxtcl50k03kkd73hmjyvkzq42h5u9qk0ew95y3l"),
            DerivedAddresses("0x16E918943416d10cB65d0955C24Ad2BAa2A79284", "9rdsZZn4fbVcPJ7YSxQsThfg8z1mQ1ErJSrULX86NZij", "bc1qsjld2m55cyzy42av9rsrfnq596wqpsljmr66t6"),
            DerivedAddresses("0x7586a05503f308A74F971De2461855edB96fF9b4", "9AmBTyKD4u8E2SWWA2hV3aGRDvynSCqrwgJRqfWPUDHN", "bc1qtrjarylf3vhdrglmrjsy7xn5zgs0q4qh0x77re"),
        )

        expectedAddresses.forEachIndexed { index, expected ->
            assertEquals(expected.eth, privateKeyToAddress(spendKey, Constants.ChainId.ETHEREUM_CHAIN_ID, index))
            assertEquals(expected.sol, privateKeyToAddress(spendKey, Constants.ChainId.SOLANA_CHAIN_ID, index))
            assertEquals(expected.btc, privateKeyToAddress(spendKey, Constants.ChainId.BITCOIN_CHAIN_ID, index))
        }
    }
}
