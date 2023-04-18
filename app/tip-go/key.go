package crypto

import "crypto/ed25519"

func SignEd25519(message, seed []byte) []byte {
	priv := ed25519.NewKeyFromSeed(seed)
	return ed25519.Sign(priv, message)
}

func VerifyEd25519(message, sig, pub []byte) bool {
	pk := ed25519.PublicKey(pub)
	return ed25519.Verify(pk, message, sig)
}
