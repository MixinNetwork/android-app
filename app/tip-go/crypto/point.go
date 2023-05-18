package crypto

import (
	"fmt"

	"github.com/btcsuite/btcd/btcutil/base58"
	"github.com/drand/kyber"
	"github.com/drand/kyber/pairing/bn256"
	"github.com/drand/kyber/sign/bls"
)

const (
	version = 'T'
)

type Point struct {
	point kyber.Point
}

func (p *Point) Verify(msg, sig []byte) error {
	scheme := bls.NewSchemeOnG1(bn256.NewSuiteG2())
	return scheme.Verify(p.point, msg, sig)
}

func (p *Point) PublicKeyString() string {
	b := p.PublicKeyBytes()
	return base58.CheckEncode(b, version)
}

func (p *Point) PublicKeyBytes() []byte {
	b, err := p.point.MarshalBinary()
	if err != nil {
		panic(err)
	}
	return b
}

func PubKeyFromBytes(b []byte) (*Point, error) {
	suite := bn256.NewSuiteG2()
	point := suite.G2().Point()
	err := point.UnmarshalBinary(b)
	return &Point{point}, err
}

func PubKeyFromBase58(s string) (*Point, error) {
	b, ver, err := base58.CheckDecode(s)
	if err != nil {
		return nil, err
	}
	if ver != version {
		return nil, fmt.Errorf("invalid version %d", ver)
	}
	return PubKeyFromBytes(b)
}
