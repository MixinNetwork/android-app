package tip

import (
	"encoding/hex"

	"github.com/drand/kyber"
	"github.com/drand/kyber/pairing/bn256"
	"github.com/drand/kyber/sign/bls"
)

type Scalar struct {
	scalar kyber.Scalar
}

func (s *Scalar) Sign(msg []byte) ([]byte, error) {
	scheme := bls.NewSchemeOnG1(bn256.NewSuiteG2())
	return scheme.Sign(s.scalar, msg)
}

func (s *Scalar) PrivateKeyBytes() []byte {
	b, err := s.scalar.MarshalBinary()
	if err != nil {
		panic(err)
	}
	return b
}

func (s *Scalar) PublicKey() *Point {
	suite := bn256.NewSuiteG2()
	return &Point{suite.Point().Mul(s.scalar, nil)}
}

func PrivateKeyFromHex(s string) (*Scalar, error) {
	seed, err := hex.DecodeString(s)
	if err != nil {
		return nil, err
	}
	suite := bn256.NewSuiteG2()
	scalar := suite.Scalar().SetBytes(seed)
	return &Scalar{scalar}, nil
}

func (s *Scalar) SetBytes(b []byte) {
	s.scalar.SetBytes(b)
}
