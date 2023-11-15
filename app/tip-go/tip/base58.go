package tip

import "github.com/btcsuite/btcd/btcutil/base58"

func Base58Decode(b string) []byte {
	return base58.Decode(b)
}

func Base58Encode(b []byte) string {
	return base58.Encode(b)
}
