package kernel

import (
	"encoding/base64"
	"encoding/json"
	"log"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestBuildWithdrawalTx(t *testing.T) {
	assert := assert.New(t)
	assetId := "5b9d576914e71e2362f89bb867eb69084931eb958f9a3622d776b861602275f4"
	amount := "1"
	address := "TV9mvdJv61mVtEoTY5h6kQtrvrULcFfadM"
	tag := ""
	feeAmount := ""
	feeKeys := ""
	feeMask := ""
	inputs, err := base64.RawURLEncoding.DecodeString("W3siYW1vdW50IjoiMTAiLCJoYXNoIjoiZjY0MTA2OWU0Y2NjZTYyZTcwZGU1OTI1M2MxMGY2YzUzNzRlODdjMzYxYmMzMjhiODIwNzE5ZDc5MTRmYTJlZCIsImluZGV4IjowfV0")
	assert.Nil(err)
	changeKeys := "5a21338c6e0e731afec7b87fb52447e095fb47b602bb89b0eb6ee68e8252623a"
	changeMask := "2c911e2b2cc4f847baadddee1bb4be927a3239a436c3e28f9e736f8d436f9311"
	extra := ""
	tx, err := BuildWithdrawalTx(assetId, amount, address, tag, feeAmount, feeKeys, feeMask, inputs, changeKeys, changeMask, extra)
	assert.Nil(err)
	d, err := json.Marshal(tx)
	assert.Nil(err)
	log.Println(string(d))
}
