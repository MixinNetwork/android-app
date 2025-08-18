package org.sol4kt.exception

class MaliciousInstructionException(message: String): RuntimeException(message) {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}