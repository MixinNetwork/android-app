package kernel

import (
	"crypto/sha512"
	"encoding/hex"
	"encoding/json"
	"strings"

	"filippo.io/edwards25519"
	"github.com/MixinNetwork/mixin/common"
	"github.com/MixinNetwork/mixin/crypto"
)

type Utxo struct {
	Hash   string `json:"hash"`
	Index  int    `json:"index"`
	Amount string `json:"amount"`
}

func BuildTx(asset string, amount string, threshold int, receiverKeys string, receiverMask string, inputs []byte, changeKeys string, changeMask string, extra string) string {
	keys := strings.Split(receiverKeys, ",")
	rks := []*crypto.Key{}
	for _, k := range keys {
		key := k
		rk, err := crypto.KeyFromString(key)
		if err != nil {
			panic(err)
		}
		rks = append(rks, &rk)
	}
	ckeys := strings.Split(changeKeys, ",")
	cks := []*crypto.Key{}
	for _, k := range ckeys {
		ke := k
		rk, err := crypto.KeyFromString(ke)
		if err != nil {
			panic(err)
		}
		cks = append(cks, &rk)
	}
	var utxo []Utxo
	err := json.Unmarshal(inputs, &utxo)
	if err != nil {
		panic(err)
	}
	ins := []*common.UTXO{}
	for _, u := range utxo {
		ut := u
		h, err := crypto.HashFromString(ut.Hash)
		if err != nil {
			panic(err)
		}
		amount := common.NewIntegerFromString(ut.Amount)
		u := common.UTXO{
			Input: common.Input{
				Hash:  h,
				Index: ut.Index,
			},
			Output: common.Output{
				Amount: amount,
			},
		}
		ins = append(ins, &u)
	}
	return buildTransaction(asset, amount, threshold, rks, receiverMask, ins, cks, changeMask, extra)
}

func buildTransaction(asset string, am string, threshold int, receiverKeys []*crypto.Key, receiverMask string, inputs []*common.UTXO, changeKeys []*crypto.Key, changeMask string, extra string) string {
	assetHash, err := crypto.HashFromString(asset)
	if err != nil {
		panic(err)
	}
	amount := common.NewIntegerFromString(am)
	var total common.Integer
	tx := common.NewTransactionV5(assetHash)
	for _, in := range inputs {
		tx.AddInput(in.Hash, in.Index)
		total = total.Add(in.Amount)
	}
	if total.Cmp(amount) < 0 {
		panic("total")
	}
	mask, err := crypto.KeyFromString(receiverMask)
	if err != nil {
		panic(err)
	}
	if !mask.CheckKey() {
		panic("invalid mask")
	}
	output := &common.Output{
		Type:   common.OutputTypeScript,
		Amount: amount,
		Keys:   receiverKeys,
		Mask:   mask,
		Script: common.NewThresholdScript(uint8(threshold)),
	}
	tx.Outputs = append(tx.Outputs, output)

	if total.Cmp(amount) > 0 {
		change := total.Sub(amount)
		script := common.NewThresholdScript(1)
		cm, err := crypto.KeyFromString(changeMask)
		if err != nil {
			panic(err)
		}
		out := &common.Output{
			Type:   common.OutputTypeScript,
			Amount: change,
			Script: script,
			Mask:   cm,
			Keys:   changeKeys,
		}
		tx.Outputs = append(tx.Outputs, out)
	}
	if extra != "" {
		ext := []byte(extra)
		if len(ext) > 512 {
			panic("extra length")
		}
		tx.Extra = ext
	}
	ver := tx.AsVersioned()
	return hex.EncodeToString(ver.Marshal())
}

func SignTx(raw string, viewKeys string, spendKey string) string {
	views := strings.Split(viewKeys, ",")
	rawBytes, err := hex.DecodeString(raw)
	if err != nil {
		panic(err)
	}
	ver, err := common.UnmarshalVersionedTransaction(rawBytes)
	if err != nil {
		panic(err)
	}
	msg := ver.PayloadHash()

	spendSeed, _ := hex.DecodeString(spendKey)
	if err != nil {
		panic("spend key seed")
	}
	h := sha512.Sum512(spendSeed)
	s, err := edwards25519.NewScalar().SetBytesWithClamping(h[:32])
	if err != nil {
		panic("ed25519: internal error: setting scalar failed")
	}
	y, err := edwards25519.NewScalar().SetCanonicalBytes(s.Bytes())
	if err != nil {
		panic(y)
	}

	for _, view := range views {
		sigs := make(map[uint16]*crypto.Signature)
		view, err := hex.DecodeString(view)
		if err != nil {
			panic(view)
		}
		x, err := edwards25519.NewScalar().SetCanonicalBytes(view)
		if err != nil {
			panic(x)
		}
		t := edwards25519.NewScalar().Add(x, y)
		var key crypto.Key
		copy(key[:], t.Bytes())
		sig := key.Sign(msg)
		// TODO valid keys index?
		sigs[uint16(0)] = &sig
		ver.SignaturesMap = append(ver.SignaturesMap, sigs)
	}
	return hex.EncodeToString(ver.Marshal())
}
