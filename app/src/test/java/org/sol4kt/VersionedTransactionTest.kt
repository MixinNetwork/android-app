package org.sol4kt

import one.mixin.android.extension.base64Encode
import org.junit.Test
import org.sol4k.instruction.TransferInstruction

class VersionedTransactionCompatTest {
    @Test
    fun testTransactionFromString() {
        // https://jup.ag/ swap
        var t = "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAoVQxhqYIKKYYeMpA6TRO9mmZnz7F7ey0Arxzusf/Es0tJwccDT11PCgZnvGncl43WttfK2QUfCBVUqNg8vpBi7S3yqkxCBRoNKvUQM6+vM7hdUBgKi+akZpbvaCpd1sVYfl6fiMQT0LnAXBDu2lQOARhtYi5QbgO4L6/gDqyD/dS+fPs/q96K8ow96krYAokWVzZaNzbWKSIcxNgQQzBKEgwkKzcQCjJktPFDq/uMmm1vR0JPHfzTSU/YmDHMVPYs3qLLQ4QY0S20HU2ioqmnunsWIpHYgUUVifOcbOi5XS4HL5/Tq7ETeVhqOTtChh/pFHz+eEhBUyQfl0VMvc6/zgMjqi5tCwzl46rfpfq7Ar6aeSwFEFdHMOzjAsCPJTqq91ipydsU+eIhTH/m/TKngg0D0n/6oHyWCREj1ntWq4ZfgINIPkc3KG4eh6BHDA91d4BVdrP+dBe5F+DHttZ3bCQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALzjEbkBDAlaM77NkXMPfqXNLSveCkWI7UEgNs31WEWCMlyWPTiSJ8bs9ECkUjg2DC1oTmdr/EIQEjnvY2+n4WaXVyp4Ez121kLcUui/jLLFZEz/BwZK3Ilf9B9OcsEAeAwZGb+UhFzL/7K26csOb57yM5bvF9xJrLEObOkAAAAC0P/on9df2SnTAmx8pWHneSwmrNt/J3VFLMhqns4zl6Mb6evO+2606PWXzaqvJdDGxu+TC0vbg5HymAgNFL11hBHnVW/IxwG7udMVuzmgVB/2xst6j9I5RArHNola8E48Gm4hX/quBhPtof2NGGMA12sQ53BrrO1WYoPAAAAAAAQbd9uHXZaGT2cvhRs7reawctIXtX1s3kTqM9YV+/wCpfwvmlw/gXULgLIhT912jP0NhVJRdx73Gp6B8AFCvBgsIDwAFAvPQAgAPAAkDjA4AAAAAAAANBgAFABMLFAEBCwIABQwCAAAAgJaYAAAAAAAUAQUBEQ0GAAkAEQsUAQESGBQABQkSERIQEg4ADAgFCQoCBBQBAwYHEiPlF8uXeuOtKgEAAAAaZAABgJaYAAAAAAA2mRQAAAAAADIAABQDBQAAAQk="
        var tx = VersionedTransactionCompat.from(t)
        var st = tx.serialize().base64Encode()
        println(st)

        var recoverTx = VersionedTransaction.from(st)
        var recoverSt = recoverTx.serialize().base64Encode()
        println(recoverSt)

        // https://app.kamino.finance/
        t = "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAQAEB0MYamCCimGHjKQOk0TvZpmZ8+xe3stAK8c7rH/xLNLStExEJwCeATGPfxBzH9gkB2fvbJIYKXNiu7XgXBjBclErN2qry+w2aU0iJe+IV8wEf1OOBiTX0EoyFNZOXVQoNwJ3pq+XM5t6yI0YkskERvUAAjCSZvYuU8EYJEmCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEsqyxEljM42gsQYuocv89+RECcS8VrxK2vmmzQ1sACAan1RcZLFxRIYzJTD1K8X9Y2u4Im6H9ROPb2YoAAAAAokLuXIau+0OyaVk/k2sXMyWzf+lyGC5upHJg9htEfJwCAwQBAAAEDQAAAABjpK0PAAAAAP4FBgAAAgUGBCh1qbBFxRcPorRMRCcAngExj38Qcx/YJAdn72ySGClzYru14FwYwXJRAA=="
        tx = VersionedTransactionCompat.from(t)
        st = tx.serialize().base64Encode()
        println(st)

        // https://jup.ag/ inject swap
        t = "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAQAHE0MYamCCimGHjKQOk0TvZpmZ8+xe3stAK8c7rH/xLNLSCQrNxAKMmS08UOr+4yabW9HQk8d/NNJT9iYMcxU9izciyZuJOP1nC3IbKGK56t2nZlUc324ou7OvBGyptHbV6SQ/JpjZTVW/dB/qyifXwON8is2s9WSzCp/JCgjKzU3BTK/ZsvSjO3z60NrEgELIM+ZR16IeIH4XePv1FtSswFNoC6Me9GAN2++ljBEvt+OIEzHF3EJpAJ4iSc8nMs8Dg3DmAyVRNthaJDgRfMKharaGbCMpO1XlOJm4TZ5h/wVLhtWfPgeJJbk/OEPgfJ5LHxDDhjdC/6XbIhitzqc3IiGaQqIFCi2LQjNLHE7iFl3CQPoYC51XYYiIxkRSkukzetYqcnbFPniIUx/5v0yp4INA9J/+qB8lgkRI9Z7VquGX2c6M7eUE8wgiKGUUCSTLcHsq1g/80ngzAnB26gkoV/fvsb6a1NIuoxH6TC+wWuD/jXXfVTXqZ8RChvhmX2lYIgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAwZGb+UhFzL/7K26csOb57yM5bvF9xJrLEObOkAAAAAEedVb8jHAbu50xW7OaBUH/bGy3qP0jlECsc2iVrwTjwbd9uHXZaGT2cvhRs7reawctIXtX1s3kTqM9YV+/wCpjJclj04kifG7PRApFI4NgwtaE5na/xCEBI572Nvp+Fm0P/on9df2SnTAmx8pWHneSwmrNt/J3VFLMhqns4zl6Mb6evO+2606PWXzaqvJdDGxu+TC0vbg5HymAgNFL11hZXkb+d5T/iKmHjh09QXKUOy9vDE3zcmK+dF3a7wV2QIHDQAFApQHBgANAAkDoAUAAAAAAAAQBgABACQMDwEBDAIAAQwCAAAAgJaYAAAAAAAPAQEBEQ44DwIAAQQFCSQSDg4RDiICAhcjGhgbFhkEByIiDwwiCA4mJR8dAwccHgInDw4gDwIUAxUFEwYLCiEvwSCbM0HWnIEMAwAAACcBZAABEgBkAQIRAWQCA4CWmAAAAAAAAa8XAAAAAAAyAAAPAwEAAAEJA/r1+yMb0OMNH8XgcXdh2+UQ3xOlp/30+dMwR+h+n2TYA7S6tgK5t+3dKUfv1QkwGEIGIlChzjSZi36q+zswgBSiBv39sHcGBhMUERgXEgIWFe9HCUmcd7rIEBNvIA9PMRhQEhK3Q0o0X+2ctQkraSRbBLi0tpoEGJmYsw=="
        tx = VersionedTransactionCompat.from(t)
        st = tx.serialize().base64Encode()
        println(st)

        // tesSolTransfer result
        t = "ARVew6hYoHo5kaSjjZoEOMEGLzRQpLW8JJkxuowUlyUxh14/6/N9+UfAEd7emdBPu/DRN1S8lLnuBb6snp1ooA0BAAEDqOPlXkyTIzkq9/wJeq73vTcF66CM2+M+bT2itX2eHXx5cm2lLZnWCwfq1zsvbwv2CDzIXHepTjTWkdePi8r+yQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAXFUebKYgYvgYhDu1MxWJbVizM8Bod2OkxO9ubs/ESZcBAgIAAQwCAAAAZAAAAAAAAAA="
        tx = VersionedTransactionCompat.from(t)
        st = tx.serialize().base64Encode()
        println(st)

        // for fix decodeLength
        t = "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAcMTa6zkpu7Tp9v4vrUgdeHOA3B6FQRXYcb/uRipjt4rIBAG4pbFZDc8JfiwLfe54mjWwcCjDSaRQLXa9uxKc5+9knX0ooRKMcRJ7a5/BB1OqOk85GnGDmZz2c2uUCrUGW5vobFjgPcSxt1XeX+ZmxQPO0rjc39p81QCTnyFWtLZCcM2/AmU5ychxesFU0wxJLDnZUnLVbYCdm7+mGSgsFsYAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAjJclj04kifG7PRApFI4NgwtaE5na/xCEBI572Nvp+FkDBkZv5SEXMv/srbpyw5vnvIzlu8X3EmssQ5s6QAAAALzps+HUwAuRBZiFAsZZ57J8TZdMfgcr3lCC8a0k+9DuBqfVFxksXFEhjMlMPUrxf1ja7gibof1E49vZigAAAAAG3fbh12Whk9nL4UbO63msHLSF7V9bN5E6jPWFfv8AqQhB0d7HiNCMwORI0tbfevaUzRxaTVZ2zpFHVZHhiL8uin7C1rHQjsA5UqMzCNMfwyJJPy92CTuFdPkTkJLUOTwEBgcAAQALBQoJAAgHBAMCAQAKBdwDTrFie9IVu1NeJgAAAAAAAAAAAAAAAAAADgAAAOumD0FpMYKrdxpT3LtXNp433hTXM7bDOUL6ycVgOynIYTg+iEsxGFEBLWKsLJcf65vQydFHOc1ATZcLsQRWOuXTpfiGqb6QnECsZedyYfbp3dqi+Ezi+Fjh0DMevsr9QCWvhdN3uwCkjT2KApX5PJ+vlD70f7Ka34f75Q4tuTP9cRHAE+JKd4eaty3mZEsu5+AK66msmiGTVL1YERqXgJsPWhucdkauSg+8CR+7e3jseBkJ+qGeBmyWGj5fL+EKnWGWnjejd7c22px0N+Ua3TbrBgEAamii82A9vhnBwXus3GKvRhTYVDz0X0Vqvh8+9DZlRkm2lun3lB5AUvdJn3m3nVRa+OZ8YVYYOAdhlm+egZJHxnnelupQPtUTJE/L3aJw/pg4hqFL9Pj0FXV1E4CdDiJwpjX4lsk+IIx5CIs4dRnkOzUQ6NVcTSfHqjCJ2MLqlHWQLee0cMV6X1MToWB/ywRRDEK+l/41c/3lLqXzzs5Mv2SGhRRZIXKD1sWEBCMr/et3tlvb7xMjP13N2efZhaKGuIsJR5FNCrcF1To/ywNZpzBaEQStrMG4w9fTNa6jB4E2Dk1/qxKPqFDbAdEHAAUC8EkCAAcACQMZ8gEAAAAAAA=="
        tx = VersionedTransactionCompat.from(t)
        st = tx.serialize().base64Encode()
        println(st)
    }

    @Test
    fun testCalcFee() {
        var t = "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAQAHE0MYamCCimGHjKQOk0TvZpmZ8+xe3stAK8c7rH/xLNLSCQrNxAKMmS08UOr+4yabW9HQk8d/NNJT9iYMcxU9izciyZuJOP1nC3IbKGK56t2nZlUc324ou7OvBGyptHbV6SQ/JpjZTVW/dB/qyifXwON8is2s9WSzCp/JCgjKzU3BTK/ZsvSjO3z60NrEgELIM+ZR16IeIH4XePv1FtSswFNoC6Me9GAN2++ljBEvt+OIEzHF3EJpAJ4iSc8nMs8Dg3DmAyVRNthaJDgRfMKharaGbCMpO1XlOJm4TZ5h/wVLhtWfPgeJJbk/OEPgfJ5LHxDDhjdC/6XbIhitzqc3IiGaQqIFCi2LQjNLHE7iFl3CQPoYC51XYYiIxkRSkukzetYqcnbFPniIUx/5v0yp4INA9J/+qB8lgkRI9Z7VquGX2c6M7eUE8wgiKGUUCSTLcHsq1g/80ngzAnB26gkoV/fvsb6a1NIuoxH6TC+wWuD/jXXfVTXqZ8RChvhmX2lYIgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAwZGb+UhFzL/7K26csOb57yM5bvF9xJrLEObOkAAAAAEedVb8jHAbu50xW7OaBUH/bGy3qP0jlECsc2iVrwTjwbd9uHXZaGT2cvhRs7reawctIXtX1s3kTqM9YV+/wCpjJclj04kifG7PRApFI4NgwtaE5na/xCEBI572Nvp+Fm0P/on9df2SnTAmx8pWHneSwmrNt/J3VFLMhqns4zl6Mb6evO+2606PWXzaqvJdDGxu+TC0vbg5HymAgNFL11hZXkb+d5T/iKmHjh09QXKUOy9vDE3zcmK+dF3a7wV2QIHDQAFApQHBgANAAkDoAUAAAAAAAAQBgABACQMDwEBDAIAAQwCAAAAgJaYAAAAAAAPAQEBEQ44DwIAAQQFCSQSDg4RDiICAhcjGhgbFhkEByIiDwwiCA4mJR8dAwccHgInDw4gDwIUAxUFEwYLCiEvwSCbM0HWnIEMAwAAACcBZAABEgBkAQIRAWQCA4CWmAAAAAAAAa8XAAAAAAAyAAAPAwEAAAEJA/r1+yMb0OMNH8XgcXdh2+UQ3xOlp/30+dMwR+h+n2TYA7S6tgK5t+3dKUfv1QkwGEIGIlChzjSZi36q+zswgBSiBv39sHcGBhMUERgXEgIWFe9HCUmcd7rIEBNvIA9PMRhQEhK3Q0o0X+2ctQkraSRbBLi0tpoEGJmYsw=="
        var tx = VersionedTransactionCompat.from(t)
        var fee = tx.calcFee("")
        println(fee.toPlainString())
    }

    @Test
    fun testSetPriorityFee() {
        val payer = PublicKey("5TDMKU3basuWC9sb9xAJgvn17KYFTLk9srPifmjZqJH9")
        val receiver = PublicKey("9B5XszUGdMaxCZ7uSQhPzdks5ZQSmWxrmzCSvtJ6Ns6g")
        val instruction = TransferInstruction(payer, receiver, 100L)
        val transaction = Transaction(payer.toBase58(), instruction, payer)
        transaction.addPlaceholderSignature()
        var tx = VersionedTransactionCompat.from(transaction.serialize().base64Encode())
        var fee = tx.calcFee("")
        println("fee $fee")
        var success = tx.setPriorityFee("20000", "600000")
        println("setPriorityFee success $success")
        fee  = tx.calcFee("")
        println("new fee $fee")
        println("new tx ${tx.serialize().base64Encode()}")

        var t = "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAoVQxhqYIKKYYeMpA6TRO9mmZnz7F7ey0Arxzusf/Es0tJwccDT11PCgZnvGncl43WttfK2QUfCBVUqNg8vpBi7S3yqkxCBRoNKvUQM6+vM7hdUBgKi+akZpbvaCpd1sVYfl6fiMQT0LnAXBDu2lQOARhtYi5QbgO4L6/gDqyD/dS+fPs/q96K8ow96krYAokWVzZaNzbWKSIcxNgQQzBKEgwkKzcQCjJktPFDq/uMmm1vR0JPHfzTSU/YmDHMVPYs3qLLQ4QY0S20HU2ioqmnunsWIpHYgUUVifOcbOi5XS4HL5/Tq7ETeVhqOTtChh/pFHz+eEhBUyQfl0VMvc6/zgMjqi5tCwzl46rfpfq7Ar6aeSwFEFdHMOzjAsCPJTqq91ipydsU+eIhTH/m/TKngg0D0n/6oHyWCREj1ntWq4ZfgINIPkc3KG4eh6BHDA91d4BVdrP+dBe5F+DHttZ3bCQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALzjEbkBDAlaM77NkXMPfqXNLSveCkWI7UEgNs31WEWCMlyWPTiSJ8bs9ECkUjg2DC1oTmdr/EIQEjnvY2+n4WaXVyp4Ez121kLcUui/jLLFZEz/BwZK3Ilf9B9OcsEAeAwZGb+UhFzL/7K26csOb57yM5bvF9xJrLEObOkAAAAC0P/on9df2SnTAmx8pWHneSwmrNt/J3VFLMhqns4zl6Mb6evO+2606PWXzaqvJdDGxu+TC0vbg5HymAgNFL11hBHnVW/IxwG7udMVuzmgVB/2xst6j9I5RArHNola8E48Gm4hX/quBhPtof2NGGMA12sQ53BrrO1WYoPAAAAAAAQbd9uHXZaGT2cvhRs7reawctIXtX1s3kTqM9YV+/wCpfwvmlw/gXULgLIhT912jP0NhVJRdx73Gp6B8AFCvBgsIDwAFAvPQAgAPAAkDjA4AAAAAAAANBgAFABMLFAEBCwIABQwCAAAAgJaYAAAAAAAUAQUBEQ0GAAkAEQsUAQESGBQABQkSERIQEg4ADAgFCQoCBBQBAwYHEiPlF8uXeuOtKgEAAAAaZAABgJaYAAAAAAA2mRQAAAAAADIAABQDBQAAAQk="
        tx = VersionedTransactionCompat.from(t)
        fee = tx.calcFee("")
        println("fee $fee")
        success = tx.setPriorityFee("20000", "600000")
        println("setPriorityFee success $success")
        fee  = tx.calcFee("")
        println("new fee $fee")
        println("new tx ${tx.serialize().base64Encode()}")
    }
}
