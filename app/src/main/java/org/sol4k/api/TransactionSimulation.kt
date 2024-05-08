package org.sol4k.api

sealed class TransactionSimulation

class TransactionSimulationError(val error: String) : TransactionSimulation()

class TransactionSimulationSuccess(val logs: List<String>) : TransactionSimulation()
