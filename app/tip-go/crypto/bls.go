package crypto

import (
	"encoding/hex"
	"strings"

	"github.com/drand/kyber"
	"github.com/drand/kyber/pairing/bn256"
	"github.com/drand/kyber/share"
	"github.com/drand/kyber/sign/tbls"
)

func RecoverSignature(partialStrings string, commitmentStrings string, assignor []byte, signersLen int) ([]byte, error) {
	partialSlice := strings.Split(partialStrings, ",")
	var partials [][]byte
	for i := range partialSlice {
		b, _ := hex.DecodeString(partialSlice[i])
		partials = append(partials, b[:])
	}

	commitmentSlice := strings.Split(commitmentStrings, ",")
	var commitments []kyber.Point
	for i := range commitmentSlice {
		point, _ := PubKeyFromBase58(commitmentSlice[i])
		commitments = append(commitments, point.point)
	}

	suite := bn256.NewSuiteG2()
	scheme := tbls.NewThresholdSchemeOnG1(bn256.NewSuiteG2())
	poly := share.NewPubPoly(suite, suite.Point().Base(), commitments)
	sig, err := scheme.Recover(poly, assignor, partials, len(commitments), signersLen)
	return sig, err
}
