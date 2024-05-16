package org.sol4k

import org.json.JSONObject

data class SignInInput(
    val domain: String,
    val address: String,
    val statement: String? = null,
    val uri: String? = null,
    val version: String? = null,
    val chainId: String? = null,
    val nonce: String? = null,
    val issuedAt: String? = null,
    val expirationTime: String? = null,
    val notBefore: String? = null,
    val requestId: String? = null,
    val resources: List<String>? = null,
) {
    fun toMessage(): String {
        val s = StringBuilder()
        s.append("$domain wants you to sign in with your Solana account:\n")
            .append(address)
        if (!statement.isNullOrBlank()) {
            s.append("\n\n$statement")
        }

        val fields = mutableListOf<String>()
        if (!uri.isNullOrBlank()) {
            fields.add("URI: $uri")
        }
        if (!version.isNullOrBlank()) {
            fields.add("Version: $version")
        }
        if (!chainId.isNullOrBlank()) {
            fields.add("Chain ID: $chainId")
        }
        if (!nonce.isNullOrBlank()) {
            fields.add("Nonce: $nonce")
        }
        if (!issuedAt.isNullOrBlank()) {
            fields.add("Issued At: $issuedAt")
        }
        if (!expirationTime.isNullOrBlank()) {
            fields.add("Expiration Time: $expirationTime")
        }
        if (!notBefore.isNullOrBlank()) {
            fields.add("Not Before: $notBefore")
        }
        if (!requestId.isNullOrBlank()) {
            fields.add("Request ID: $requestId")
        }
        if (!resources.isNullOrEmpty()) {
            fields.add("Resources:")
            for (r in resources) {
                fields.add("- $r")
            }
        }
        if (fields.isNotEmpty()) {
            s.append("\n\n${fields.joinToString("\n")}")
        }

        return s.toString()
    }

    companion object {
        private val validChainIds = listOf("mainnet", "testnet", "devnet", "localnet", "solana:mainnet", "solana:testnet", "solana:devnet")

        fun from(jsonData: String, addr: String? = null): SignInInput {
            val o = JSONObject(jsonData)
            val domain = if (o.has("domain")) {
                o.getString("domain")
            } else {
                throw SignInException("empty domain")
            }
            val address = if (o.has("address")) {
                o.getString("address")
            } else {
                if (addr.isNullOrBlank()) {
                    throw SignInException("empty address")
                } else {
                    addr
                }
            }
            val statement = if (o.has("statement")) {
                o.getString("statement")
            } else null
            val uri = if (o.has("uri")) {
                o.getString("uri")
            } else null
            val version = if (o.has("version")) {
                o.getString("version")
            } else null
            val chainId = if (o.has("chainId")) {
                val c = o.getString("chainId")
                if (!c.isNullOrBlank() && !validChainIds.contains(c)) {
                    throw SignInException("invalid chainId $c")
                }
                c
            } else null
            val nonce = if (o.has("nonce")) {
                o.getString("nonce")
            } else null
            val issuedAt = if (o.has("issuedAt")) {
                o.getString("issuedAt")
            } else null
            val expirationTime = if (o.has("expirationTime")) {
                o.getString("expirationTime")
            } else null
            val notBefore = if (o.has("notBefore")) {
                o.getString("notBefore")
            } else null
            val requestId = if (o.has("requestId")) {
                o.getString("requestId")
            } else null
            val resources = if (o.has("resources")) {
                val arr = o.getJSONArray("resources")
                val rs = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    rs.add(arr.getString(i))
                }
                rs
            } else emptyList()

            return SignInInput(domain, address, statement, uri, version, chainId, nonce, issuedAt, expirationTime, notBefore, requestId, resources)
        }
    }
}

data class SignInOutput(
    val account: SignInAccount,
    val signature: String,
    val signedMessage: String,
    val signatureType: String = "ed25519",
)

data class SignInAccount(
    val publicKey: String,
)

class SignInException(message: String) : IllegalArgumentException(message) {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}