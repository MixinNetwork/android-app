package one.mixin.android.crypto.bls

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.crypto.blst.BLST_ERROR
import one.mixin.android.crypto.blst.P1
import one.mixin.android.crypto.blst.P1_Affine
import one.mixin.android.crypto.blst.P2
import one.mixin.android.crypto.blst.P2_Affine
import one.mixin.android.crypto.blst.Pairing
import one.mixin.android.crypto.blst.SecretKey
import one.mixin.android.crypto.blst.aggregateVerify
import one.mixin.android.crypto.blst.blsDST
import one.mixin.android.crypto.blst.fromHexString
import one.mixin.android.crypto.blst.sign
import one.mixin.android.crypto.blst.toHexString
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class BlsTest {

    @Test
    fun testSigVerify() {
        val m = "abc".toByteArray()
        val dst = ""
        val sk = SecretKey()
        sk.keygen("*".repeat(32).toByteArray())
        val skBytes = sk.to_bendian()
        println("skHex ${toHexString(skBytes)}")

        val pk = P1(sk)
        val pkBytes = pk.compress()
        println("pkHex ${toHexString(pkBytes)}")

        val sig = P2()
        sig.hash_to(m, dst)
            .sign_with(sk)
        val sigBytes = sig.compress()
        println("sigHex ${toHexString(sigBytes)}")

        val pkA = P1_Affine(pk)
        val pkABytes = pkA.compress()
        println("pkAHex ${toHexString(pkABytes)}")

        val sigA = P2_Affine(sig)
        val sigABytes = sigA.compress()
        println("sigAHex ${toHexString(sigABytes)}")

        val result = sigA.core_verify(pkA, true, m, dst)
        assert(result == BLST_ERROR.BLST_SUCCESS)
    }

    @Test
    fun testVerifyAggregate() {
        val msgCount = 5
        for (size in 1 until 20) {
            println("size $size")
            val msgs = arrayListOf<ByteArray>()
            val sks = arrayListOf<SecretKey>()
            val pks = arrayListOf<P1_Affine>()

            for (i in 0 until msgCount) {
                msgs.add("abc $i $size".toByteArray())
            }
            for (i in 0 until size) {
                val sk = SecretKey()
                sk.keygen("*".repeat(32).toByteArray())
                sks.add(sk)
                pks.add(P1(sk).to_affine())
            }

            for (i in 0 until msgCount) {
                val sigsToAgg = arrayListOf<P2_Affine>()
                val pksToAgg = arrayListOf<P1_Affine>()
                for (j in 0 until size) {
                    val sig = sign(msgs[i], sks[j]).to_affine()
                    sigsToAgg.add(sig)
                    pksToAgg.add(pks[j])
                }

                aggregateVerify(msgs[i], pks, sigsToAgg)
            }
        }
    }

    @Test
    fun testAggregateVectors() {
        val pkss = arrayOf(
            arrayOf(
                "983eb5e80d6f7d664dae1cfe503b373c1547a3398beadaf044ed152bf121c98f4311dafc71bd0d65054ae926855b9047",
                "870546f4562e999a984f60e0c6a981ea56ad52398c41ee821069c5c0bf38e2973a772c2c8af365d6909b8f46264c8109",
                "91dedb6aaedd960e4659f929e6237d6755562f6d38c3312fbab9da7399ec7cd17407272ae62c788fdc836496a1808cc1",
                "8c3ec9a46707546cb32328168a8344b9ce5229f85679711a8aa2380d7de9dda166caf35cd5a17ad1a0ab976bffd0f615",
                "b3a1437129ac211cbca2e60faa3c9ac130dad6c64a45ed7355509418bf61a98c7479e9b905adeea05a12b9032d90386e",
            ),
            arrayOf(
                "b95fb187e1a76417286293d23bced7eef2412dc073d46bb5edc4daf1b804ae8748183b8b6b179a21d1be2a100e8fe4a8",
                "a6ae88b6d05b898a39fa41e704133909db04182ce16c842bd4adf435b02ab9fe986344982d1648ea25912b4340f0e8ca",
                "8942c1dd258d58d93475084b7a21d8b4af08ce0e51c5e442878152aa83482b0710086387e4a5896fe22ea1175c24e7fd",
                "a277a7553f06a177a36cdb51b65eae665bf58e4c292ee5bbda1766431d17f998cc43e305d17e1a31e6e094ca57dc84ad",
                "b15bb3631c8145f218f69494ad52b204b15a0f9c2e3dc818980401fdf4268a516ef74bd63a0de394ba1aea2936e505ef",
            ),
            arrayOf(
                "931aefc781ffec302957c44f2319b42d0f4dfe9889ae9484fe1e39ab9c7e3862342c969f77be4868ab8fd6a444935ec4",
                "b7d573d5396c3db6c1d28a7a7e7a0e6f651fe4177862f9f0de068a4b1320cfe329a0f76387fbe9e62e8cdfa4d8e7be54",
                "b6779f577463f6e89d131b0d3d5c2ad3cf7fd84bc75c4f1e9844e931fefcc5f6abd45b34eb9e28b4975a332e67b21a4e",
                "8a26258387b997ba96f2b9e6fb418b999091c4ff1b6737c4096d7953dfdcf6674a078add1c9e2b5cb7aa358266e66399",
                "b9e25cc45102274ab4b3cf3e010c48090f3ee10579c5a87e6346b2fcbf228cbed39d27737513aafd9b239845da86ff06",
            )
        )
        val sigss = arrayOf(
            arrayOf(
                "b6bc95d520ff0e17c059c240bb8a4af07acabf5cd446a142de1c0d0d89e9d173fcc5ca127a15bceb0d3a53a7cfdc71670fee7a875baf29e16354de8954176710290fba85cbbc88dbb264a8b913356a5614f94a23e947ae03491ed3dcbef2be78",
                "8e080ef919584b4dd97d5f229af71e10d0f7bd6c87c568c167b3dbd25716b283c4f477f326dde8a9b124406ad9f60d81140c0120dbffdf5d1219b43959b413fb24c54fbe9f359bb000a47afbd9e01a92f4c4cc975e63cf8b1334d301dbd18e7d",
                "a09a9d391c4762b038c4f465d56d2abe7d820956c08c890c40407ec0757517f7bf67057b13d7af61dfce9b9cc998d4d3179623408f1fa87bc78001ec6949a27c15cda2f709faf8d577738e03e78445841994e9b8bab3633ba358e93d6262c751",
                "8d399c5a828a5301d20a7809ef77ab1941937ebb9f42fd3284a7c2d46382bbb7a966541a317e4cac9534eceeb50da7a90fcc164dbce495b7a04049cd8348c8fa2059fba0adff0fc226d8abb7aa01c94c19ca7728af3df9cc9985d814cc8b9964",
                "949bc483b6176d6707cca91415a72c023bbc1071c911436ecdc0756778fef4c6d9fb0151c2c67621a5e04d3b65242fe401ebb662127d323f904efc726075a9c2f95d5dcf54a6ae196d8466f241518618f0c7df21e8fd1ba736e8a398d1a2ae50",
            ),
            arrayOf(
                "b74cb1587370a34dad1108a580e45b3b35dbac346330d5d533edf59d9e477a0c4bb77a85ba699025f3b2674f5e109d3903452b3da5ebbcc9f2ec94ae2167c3f01ce8062f94d1ff8986ca62f984d602b621020fbfe9cc9418fa86df0739b1d2c3",
                "a10c02654fc27df3ca521209e046229152865aa26e97122c85b934e3a9bf9ca9063e394e1da58fdc60f76a93fbca750e1746521dd0aebb35fdb74d0af91e2a5bf95f83c458af88282ba1487488a3a3d3154047629018711250d1c63a762e5c26",
                "a1c1ee1bb879a39d9047aed0b9ad97e5a249d6bd3b35e19d44bbdf01c405b5b40ccb1f32709b258e742383f92c3ed55f0a92adb96d94ac6baefda3d4c87b79fcfaac1804d6eb29d41e23a2747f29d7bf482e2c9781a68eb4058568f7c5d64a59",
                "ac93029a6ecb03d440a7e4d4f7dfc1b38ba8e26fe2c5b9b4c1846ed1a03a2d0632826b59bf78a356138cf798c3c63b4a05c5fc3bed505998afc9896966036842eb0ace3ccb9d7b61ed74491c80a823ea4dbd01adc1f4c45e984bd0aa3de29258",
                "856d865c7b4eb95d7de95612732cf29ec183b1e682842eb8427a95d938e4ca124ab541c4264067925c2e152896bed2340e8e203abe811bc54b9e75c25c4c05ef243715d86276a4af6763e0868d03ceb8ba79495337a0b93517a96f917cc2a53b",
            ),
            arrayOf(
                "938f6c0d6cd08ec36886c9ad44183d1bec7fde2b8ffcef45f30bf1c6020810976d80b8aa18748ba4fc52cf72f73d2f0510869f49c9da57375cd733763e85d117f5c36dcac7e9e4450a8861b22a70e202691983fbb32036ed270086ca8f629fff",
                "972c027b4b5e59758eb6ff915e5eecf4e168940aa8044cb26bc48f3a9c0224c13e3fec7f919a5c0d8cb693eb74669a4708d8416ba7db98ec35d69a39b0d398a39b8e50a48195e0dd9d109f943a9312a70c31ee661949e7bb1daa57592416cc50",
                "ab1648fb8cdef311157b17aac08ef630840f491ae309b694da93fa4dc42256c74485e6a7e6e45de48dfab59869e89e040d85bf5ac23125d848a1d6c4472103758a968ec2607afea012f78c2c4a994f9a45611e90317aa8e741bc3d577f509fc4",
                "a9c98893120670180be848908939bc7ed97a0cfb94d351eb70004b692433070f8af087e10a03909090af7631c4a672990c8c7d4fc239003bcba6c3723272913b07e4f4bddde7ea77295c71e7aeef069b6eb1ff015de5a5f2ad6a1577d1236c9f",
                "b9ef1c2f02b906f8f89f19e1bd1b8f1c3d67a18e50ac840c0743c4d2e947046be997d16d53560dc12d401392fa88800b10fabfeadb3867a69548df5ab0a572f5f2155950a8a4eef79b9fc8f240e6bbb98645ecf83c9f6d0597eb1c54cf24cc3d",
            )
        )
        val aggSigs = arrayOf(
            "b08d51b1641bb2c9c30df1add3d4669701655ab2c7d902335a398e478d3770090a5e2b6112e752069548cf93aca601cc07cdb01c328eb16d34fc0d66edec9995253c027d0abc8df6ba21b4955640812d1ce36dee445f6db6c396d064bf213190",
            "b4f4f267e4248bc05b2d284fed328a8da18d58addd06590aa0a4a8bbdd82988eb731b21a4ba31d26ca105a52f21c604606c16c7580edc686bd229bed77e9fe20668d4c9eda888600bcd73f2aa706ff6c3d5361c4a1a4bf1e78e2b6417a4c8539",
            "acb8a496b4ca7f4e599997944b625c9126ba9ff9cbec50c4644948ff66c549ea1f6348137dc5009672b905f4c22140d518809a5dda8163ddc11999b1624cdbe1ce65823fbe7f1e0ba93e60d977fcacb10b67a8340667b8d4ddaf5aa2f9b128bb",
        )
        val aggPks = arrayOf(
            "96cda92cc499ace5120f641940b5119b98352396aa47acbd1fab3f93a3926c730092e6154d8a948a7ff0c44dd70986e9",
            "900dd59adcc331ca9426dc0bf8be48aa465cad36ca6018a524c23f42d4a926340c52ae51f2c3d54d5de6a45137ee5ef6",
            "99cd8687f20ae2b76ded973724ea1a0f177520197f9c440cc16b379c75f5f2ee1d8cfb372edf114db86bbff4628497b8",
        )
        val msg = "abc".toByteArray()

        val sigsLen = sigss.size
        for (i in 0 until sigsLen) {
            val sigs = sigss[i]
            val pks = pkss[i]

            val aggSig = P2()
            for (s in sigs) {
                val sig = P2(fromHexString(s))
                aggSig.aggregate(sig.to_affine())
            }
            val aggSigHex = toHexString(aggSig.compress())
            assert(aggSigHex == aggSigs[i])

            val aggPk = P1()
            for (p in pks) {
                val pk = P1(fromHexString(p))
                aggPk.aggregate(pk.to_affine())
            }
            val aggPkHex = toHexString(aggPk.compress())
            assert(aggPkHex == aggPks[i])

            val afSig = aggSig.to_affine()
            val afPk = aggPk.to_affine()
            val result =
                afSig.core_verify(afPk, true, msg, blsDST)
            assert(result == BLST_ERROR.BLST_SUCCESS)
        }
    }

    @Test
    fun testAgg() {
        val msg = "assertion".toByteArray()
        val DST = "MY-DST"

        val SK = SecretKey()
        SK.keygen("*".repeat(32).toByteArray())

        // generate public key and serialize it...

        // generate public key and serialize it...
        val pk_ = P1(SK)
        val pk_for_wire = pk_.serialize()

        // sign |msg| and serialize the signature...

        // sign |msg| and serialize the signature...
        val sig_ = P2()
        val sig_for_wire = sig_.hash_to(msg, DST, pk_for_wire)
            .sign_with(SK)
            .serialize()

        // now on "receiving" side, start with deserialization...

        // now on "receiving" side, start with deserialization...
        val _sig = P2_Affine(sig_for_wire)
        val _pk = P1_Affine(pk_for_wire)
        if (!_pk.in_group()) throw RuntimeException("disaster")
        val ctx = Pairing(true, DST)
        ctx.aggregate(_pk, _sig, msg, pk_for_wire)
        ctx.commit()
        if (!ctx.finalverify()) throw RuntimeException("disaster")
        println("OK")
    }
}
