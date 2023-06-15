package crypto

import (
	"crypto/ed25519"

	"filippo.io/edwards25519"
)

func SignEd25519(message, seed []byte) []byte {
	priv := ed25519.NewKeyFromSeed(seed)
	return ed25519.Sign(priv, message)
}

func VerifyEd25519(message, sig, pub []byte) bool {
	pk := ed25519.PublicKey(pub)
	return ed25519.Verify(pk, message, sig)
}

func GenerateKey() []byte {
	_, priv, err := ed25519.GenerateKey(nil)
	if err != nil {
		panic(err)
	}
	return priv
}

func NewKeyFromSeed(seed []byte) []byte {
	return ed25519.NewKeyFromSeed(seed)
}

func PublicKeyToCurve25519(pub []byte) ([]byte, error) {
	publicKey := ed25519.PublicKey(pub)
	p, err := (&edwards25519.Point{}).SetBytes(publicKey[:])
	if err != nil {
		return nil, err
	}
	return p.BytesMontgomery(), nil
}
