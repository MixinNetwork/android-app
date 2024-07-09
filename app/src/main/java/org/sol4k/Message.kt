package org.sol4k

import okio.Buffer
import org.sol4k.VersionedTransaction.Companion.PUBLIC_KEY_LENGTH
import org.sol4k.instruction.CompiledInstruction
import org.sol4k.instruction.SetComputeUnitLimitInstruction
import org.sol4k.instruction.SetComputeUnitPriceInstruction

data class Message(
    val version: MessageVersion,
    val header: MessageHeader,
    var accounts: List<PublicKey>,
    var recentBlockhash: String,
    var instructions: List<CompiledInstruction>,
    val addressLookupTables: List<CompiledAddressLookupTable>,
) {

    enum class MessageVersion {
        Legacy, V0
    }

    fun serialize(): ByteArray {
        val b = Buffer()
        if (version != MessageVersion.Legacy) {
            val v = version.name.substring(1).toIntOrNull()
            if (v == null || v > 255) {
                throw Exception("failed to parse message version")
            }
            if (v > 128) {
                throw Exception("unexpected message version")
            }
            b.writeByte(v.toByte() + 128.toByte())
        }

        b.writeByte(header.numRequireSignatures)
            .writeByte(header.numReadonlySignedAccounts)
            .writeByte(header.numReadonlyUnsignedAccounts)
            .write(Binary.encodeLength(accounts.size))
        for (a in accounts) {
            b.write(a.bytes())
        }
        b.write(Base58.decode(recentBlockhash))
            .write(Binary.encodeLength(instructions.size))
        for (i in instructions) {
            b.writeByte(i.programIdIndex)
                .write(Binary.encodeLength(i.accounts.size))
            for (a in i.accounts) {
                b.writeByte(a)
            }
            b.write(Binary.encodeLength(i.data.size))
                .write(i.data)
        }

        if (version != MessageVersion.Legacy) {
            var validAddressLookupCount = 0
            val accountLookupTableSerializedData = Buffer()
            for (a in addressLookupTables) {
                if (a.writableIndexes.isNotEmpty() || a.readonlyIndexes.isNotEmpty()) {
                    accountLookupTableSerializedData.write(a.publicKey.bytes())
                        .write(Binary.encodeLength(a.writableIndexes.size))
                        .write(a.writableIndexes)
                        .write(Binary.encodeLength(a.readonlyIndexes.size))
                        .write(a.readonlyIndexes)
                    validAddressLookupCount++
                }
            }

            b.write(Binary.encodeLength(validAddressLookupCount))
            b.write(accountLookupTableSerializedData.readByteString())
        }
        return b.readByteArray()
    }

    fun setPriorityFee(unitPrice: Long, unitLimit: Int) {
        val leftInstructions = instructions.filter { i ->
            accounts[i.programIdIndex] != Constants.COMPUTE_BUDGET_PROGRAM_ID || notComputeInstruction(i.data)
        }
        var programIdIdx = accounts.indexOf(Constants.COMPUTE_BUDGET_PROGRAM_ID)
        if (programIdIdx == -1) {
            programIdIdx = accounts.size
            header.numReadonlyUnsignedAccounts+=1
            accounts = accounts.toMutableList().apply {
                add(Constants.COMPUTE_BUDGET_PROGRAM_ID)
            }.toList()
        }
        val setComputeUnitLimitInstruction = SetComputeUnitLimitInstruction(unitLimit)
        val setComputeUnitPriceInstruction = SetComputeUnitPriceInstruction(unitPrice)
        val newInstructions = mutableListOf<CompiledInstruction>()
        newInstructions.add(setComputeUnitLimitInstruction.toCompiledInstruction(programIdIdx))
        newInstructions.add(setComputeUnitPriceInstruction.toCompiledInstruction(programIdIdx))
        newInstructions.addAll(leftInstructions)
        instructions = newInstructions
    }

    companion object {
        fun deserialize(d: ByteArray): Message {
            var data = d
            val v = data.first().toUByte()
            val version = if (v > 127.toUByte()) {
                data = data.drop(1).toByteArray()
                MessageVersion.V0
            } else {
                MessageVersion.Legacy
            }

            val numRequiredSignatures = data.first().toInt().also { data = data.drop(1).toByteArray() }
            val numReadonlySignedAccounts = data.first().toInt().also { data = data.drop(1).toByteArray() }
            val numReadonlyUnsignedAccounts = data.first().toInt().also { data = data.drop(1).toByteArray() }

            val accountKeyCount = Binary.decodeLength(data)
            data = accountKeyCount.second
            val accountKeys = mutableListOf<PublicKey>() // list of all accounts
            for (i in 0 until accountKeyCount.first) {
                val account = data.slice(0 until PUBLIC_KEY_LENGTH)
                data = data.drop(PUBLIC_KEY_LENGTH).toByteArray()
                accountKeys.add(PublicKey(account.toByteArray()))
            }

            val recentBlockhash = data.slice(0 until PUBLIC_KEY_LENGTH).toByteArray().also {
                data = data.drop(PUBLIC_KEY_LENGTH).toByteArray()
            }

            val instructionCount = Binary.decodeLength(data)
            data = instructionCount.second
            val instructions = mutableListOf<CompiledInstruction>()
            for(i in 0 until instructionCount.first) {
                val programIdIndex = data.first().toInt().also { data = data.drop(1).toByteArray() }

                val accountCount = Binary.decodeLength(data)
                data = accountCount.second
                val accountIndices = data.slice(0 until accountCount.first).map(Byte::toInt).also {
                    data = data.drop(accountCount.first).toByteArray()
                }

                val dataLength = Binary.decodeLength(data)
                data = dataLength.second
                val dataSlice = data.slice(0 until dataLength.first).toByteArray().also {
                    data = data.drop(dataLength.first).toByteArray()
                }
                instructions.add(
                    CompiledInstruction(
                        programIdIndex = programIdIndex,
                        data = dataSlice,
                        accounts = accountIndices,
                    )
                )
            }

            val addressLookupTables = mutableListOf<CompiledAddressLookupTable>()
            if (version == MessageVersion.V0) {
                val addressLookupTableCount = Binary.decodeLength(data)
                data = addressLookupTableCount.second
                for (i in 0 until addressLookupTableCount.first) {
                    val account = data.slice(0 until PUBLIC_KEY_LENGTH).toByteArray().also {
                        data = data.drop(PUBLIC_KEY_LENGTH).toByteArray()
                    }
                    val writableAccountIdxCount = Binary.decodeLength(data)
                    data = writableAccountIdxCount.second
                    val writableAccountIdx = data.slice(0 until writableAccountIdxCount.first).toByteArray().also {
                        data = data.drop(writableAccountIdxCount.first).toByteArray()
                    }
                    val readOnlyAccountIdxCount = Binary.decodeLength(data)
                    data = readOnlyAccountIdxCount.second
                    val readOnlyAccountIdx = data.slice(0 until readOnlyAccountIdxCount.first).toByteArray().also {
                        data = data.drop(readOnlyAccountIdxCount.first).toByteArray()
                    }
                    addressLookupTables.add(
                        CompiledAddressLookupTable(
                            publicKey = PublicKey(account),
                            writableIndexes = writableAccountIdx,
                            readonlyIndexes = readOnlyAccountIdx,
                        )
                    )
                }
            }
            return Message(
                version = version,
                header = MessageHeader(
                    numRequireSignatures = numRequiredSignatures,
                    numReadonlySignedAccounts = numReadonlySignedAccounts,
                    numReadonlyUnsignedAccounts = numReadonlyUnsignedAccounts,
                ),
                accounts = accountKeys,
                recentBlockhash = Base58.encode(recentBlockhash),
                instructions = instructions,
                addressLookupTables = addressLookupTables,
            )
        }
    }
}

data class MessageHeader(
    val numRequireSignatures: Int,
    val numReadonlySignedAccounts: Int,
    var numReadonlyUnsignedAccounts: Int,
)