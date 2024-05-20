package org.sol4k

import java.math.BigDecimal
import java.math.RoundingMode

fun lamportToSol(v: String): BigDecimal =
    lamportToSol(BigDecimal(v))

fun lamportToSol(v: BigDecimal): BigDecimal =
    v.divide(BigDecimal.TEN.pow(9)).setScale(9, RoundingMode.CEILING)

fun solToLamport(v: String): BigDecimal =
    solToLamport(BigDecimal(v))

fun solToLamport(v: BigDecimal): BigDecimal =
    v.multiply(BigDecimal.TEN.pow(9))

fun microToLamport(v: BigDecimal): BigDecimal =
    v.divide(BigDecimal.TEN.pow(6))

fun lamportToMicro(v: BigDecimal): BigDecimal =
    v.multiply(BigDecimal.TEN.pow(6))