package tip

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"io"

	"github.com/drand/kyber/pairing/bn256"
	"github.com/drand/kyber/util/random"
	"golang.org/x/crypto/sha3"
)

func Ecdh(point *Point, scalar *Scalar) []byte {
	suite := bn256.NewSuiteG2()
	if point.point.Equal(suite.Point()) {
		r := suite.Scalar().Pick(random.New())
		point.point = point.point.Mul(r, nil)
	}
	point.point = suite.Point().Mul(scalar.scalar, point.point)

	b := point.PublicKeyBytes()
	sum := sha3.Sum256(b)
	return sum[:]
}

func Decrypt(pub *Point, priv *Scalar, b []byte) []byte {
	secret := Ecdh(pub, priv)
	aes, _ := aes.NewCipher(secret)
	aead, _ := cipher.NewGCM(aes)
	nonce := b[:aead.NonceSize()]
	cipher := b[aead.NonceSize():]
	d, _ := aead.Open(nil, nonce, cipher, nil)
	return d
}

func Encrypt(pub *Point, priv *Scalar, b []byte) []byte {
	secret := Ecdh(pub, priv)
	aes, err := aes.NewCipher(secret)
	if err != nil {
		panic(err)
	}
	aead, err := cipher.NewGCM(aes)
	if err != nil {
		panic(err)
	}
	nonce := make([]byte, aead.NonceSize())
	_, err = io.ReadFull(rand.Reader, nonce)
	if err != nil {
		panic(err)
	}
	cipher := aead.Seal(nil, nonce, b, nil)
	return append(nonce, cipher...)
}
