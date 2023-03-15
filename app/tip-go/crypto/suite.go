package crypto

import (
	"github.com/drand/kyber/pairing/bn256"
)

type SuiteBn256 struct {
	suite *bn256.SuiteBn256
}

type Suite struct {
	suite *bn256.Suite
}

func NewSuiteBn256() *SuiteBn256 {
	return &SuiteBn256{suite: bn256.NewSuiteBn256()}
}

func NewSuiteG2() *Suite {
	return &Suite{bn256.NewSuiteG2()}
}

func (s *SuiteBn256) Scalar() *Scalar {
	return &Scalar{s.suite.Scalar()}
}

func (s *Suite) Scalar() *Scalar {
	return &Scalar{s.suite.Scalar()}
}
