package crypto

import (
	"encoding/hex"
	"strings"

	"github.com/drand/kyber/pairing/bn256"
	"github.com/drand/kyber/sign/bls"
)

func AggregateSignatures(sigs string) ([]byte, error) {
	sigSlice := strings.Split(sigs, ",")
	var sigBytes [][]byte
	for i := range sigSlice {
		b, _ := hex.DecodeString(sigSlice[i])
		sigBytes = append(sigBytes, b[:])
	}

	scheme := bls.NewSchemeOnG1(bn256.NewSuiteG2())
	return scheme.AggregateSignatures(sigBytes...)
}
