package tip

import (
	"bytes"
	"crypto/rand"
	"io"
	"testing"
)

func TestEncryptDecryptCBC(t *testing.T) {
	plaintext := []byte("This is a plaintext string")
	key := make([]byte, 32)
	_, err := io.ReadFull(rand.Reader, key)
	if err != nil {
		t.Fatalf("Error generating random key: %v", err)
	}

	ciphertext := EncryptCBC(plaintext, key)
	if ciphertext == nil {
		t.Fatal("Error in encryption")
	}

	decrypted := DecryptCBC(ciphertext, key)
	if decrypted == nil {
		t.Fatal("Error in decryption")
	}

	if !bytes.Equal(plaintext, decrypted) {
		t.Fatalf("The decrypted text does not match the original plaintext: expect% v, get% v", plaintext, decrypted)
	}
}
