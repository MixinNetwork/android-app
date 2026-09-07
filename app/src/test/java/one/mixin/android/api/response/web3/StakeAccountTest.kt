package one.mixin.android.api.response.web3

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import one.mixin.android.api.MixinResponse
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class StakeAccountTest {
    @Test
    fun stakeAccountParsesCreditsObservedBeyondIntRange() {
        val json =
            """
            {
              "data": [
                {
                  "pubkey": "stake-account",
                  "account": {
                    "lamports": 10705048,
                    "owner": "stake-program",
                    "rentEpoch": 18446744073709551615,
                    "data": {
                      "parsed": {
                        "info": {
                          "meta": {
                            "authorized": {
                              "staker": "wallet-address",
                              "withdrawer": "wallet-address"
                            },
                            "lockup": {
                              "custodian": "custodian-address",
                              "epoch": 0,
                              "unixTimestamp": 0
                            },
                            "rentExemptReserve": "2282880"
                          },
                          "stake": {
                            "creditsObserved": 2359243881,
                            "delegation": {
                              "activationEpoch": "765",
                              "deactivationEpoch": "18446744073709551615",
                              "stake": "8385428",
                              "voter": "validator-address"
                            }
                          }
                        },
                        "type": "delegated"
                      },
                      "program": "stake",
                      "space": 200
                    },
                    "executable": false
                  }
                }
              ]
            }
            """.trimIndent()
        val type = object : TypeToken<MixinResponse<List<StakeAccount>>>() {}.type

        val response = Gson().fromJson<MixinResponse<List<StakeAccount>>>(json, type)

        assertEquals(
            BigInteger("2359243881"),
            response.data?.single()?.account?.data?.parsed?.info?.stake?.creditsObserved,
        )
    }
}
