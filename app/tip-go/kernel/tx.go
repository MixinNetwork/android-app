package kernel

import (
	"crypto/sha512"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
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

type Tx struct {
	Hash   string `json:"hash"`
	Raw    string `json:"raw"`
	Change *Utxo  `json:"change,omitempty"`
}

func BuildTx(asset string, amount string, threshold int, receiverKeys string, receiverMask string, inputs []byte, changeKeys string, changeMask string, extra string) (string, error) {
	keys := strings.Split(receiverKeys, ",")
	rks := []*crypto.Key{}
	for _, k := range keys {
		key := k
		rk, err := crypto.KeyFromString(key)
		if err != nil {
			return "", err
		}
		rks = append(rks, &rk)
	}
	ckeys := strings.Split(changeKeys, ",")
	cks := []*crypto.Key{}
	for _, k := range ckeys {
		ke := k
		rk, err := crypto.KeyFromString(ke)
		if err != nil {
			return "", err
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
			return "", err
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

func buildTransaction(asset string, amount string, threshold int, receiverKeys []*crypto.Key, receiverMask string, inputs []*common.UTXO, changeKeys []*crypto.Key, changeMask string, extra string) (string, error) {
	assetHash, err := crypto.HashFromString(asset)
	if err != nil {
		return "", err
	}

	amountValue := common.NewIntegerFromString(amount)
	total := common.NewInteger(0)

	tx := common.NewTransactionV5(assetHash)
	for _, in := range inputs {
		tx.AddInput(in.Hash, in.Index)
		total = total.Add(in.Amount)
	}

	if total.Cmp(amountValue) < 0 {
		return "", errors.New("insufficient funds")
	}

	mask, err := crypto.KeyFromString(receiverMask)
	if err != nil {
		return "", err
	}
	if !mask.CheckKey() {
		return "", errors.New("invalid mask")
	}

	output := &common.Output{
		Type:   common.OutputTypeScript,
		Amount: amountValue,
		Keys:   receiverKeys,
		Mask:   mask,
		Script: common.NewThresholdScript(uint8(threshold)),
	}
	tx.Outputs = append(tx.Outputs, output)

	if total.Cmp(amountValue) > 0 {
		change := total.Sub(amountValue)
		script := common.NewThresholdScript(1)

		changeMaskKey, err := crypto.KeyFromString(changeMask)
		if err != nil {
			return "", err
		}

		out := &common.Output{
			Type:   common.OutputTypeScript,
			Amount: change,
			Script: script,
			Mask:   changeMaskKey,
			Keys:   changeKeys,
		}
		tx.Outputs = append(tx.Outputs, out)
	}

	if extra != "" {
		extraBytes := []byte(extra)
		if len(extraBytes) > 512 {
			return "", errors.New("extra data is too long")
		}
		tx.Extra = extraBytes
	}

	ver := tx.AsVersioned()
	return hex.EncodeToString(ver.Marshal()), nil
}

func SignTx(raw, inputKeys, viewKeys string, spendKey string) (*Tx, error) {
	views := strings.Split(viewKeys, ",")
	rawBytes, err := hex.DecodeString(raw)
	if err != nil {
		return nil, err
	}
	ver, err := common.UnmarshalVersionedTransaction(rawBytes)
	if err != nil {
		return nil, err
	}
	msg := ver.PayloadHash()

	var inputs [][]string
	if err := json.Unmarshal([]byte(inputKeys), &inputs); err != nil {
		return nil, err
	}

	spendSeed, err := hex.DecodeString(spendKey)
	if err != nil {
		return nil, err
	}
	h := sha512.Sum512(spendSeed)
	s, err := edwards25519.NewScalar().SetBytesWithClamping(h[:32])
	if err != nil {
		return nil, err
	}
	y, err := edwards25519.NewScalar().SetCanonicalBytes(s.Bytes())
	if err != nil {
		return nil, err
	}

	for i, view := range views {
		viewBytes, err := hex.DecodeString(view)
		if err != nil {
			return nil, err
		}
		x, err := edwards25519.NewScalar().SetCanonicalBytes(viewBytes)
		if err != nil {
			return nil, err
		}

		t := edwards25519.NewScalar().Add(x, y)
		var key crypto.Key
		copy(key[:], t.Bytes())

		input := inputs[i]
		keysFilter := make(map[string]uint16)
		for i, k := range input {
			keysFilter[k] = uint16(i)
		}

		i, found := keysFilter[key.Public().String()]
		if !found {
			return nil, fmt.Errorf("invalid public key for the input %s", key.Public().String())
		}
		sig := key.Sign(msg)
		sigs := make(map[uint16]*crypto.Signature)
		sigs[i] = &sig
		ver.SignaturesMap = append(ver.SignaturesMap, sigs)
	}
	var changeUtxo *Utxo
	if len(ver.Outputs) == 2 {
		changeUtxo = &Utxo{
			Hash:   ver.PayloadHash().String(),
			Amount: ver.Outputs[1].Amount.String(),
			Index:  1,
		}
	}

	transaction := &Tx{
		Hash:   ver.PayloadHash().String(),
		Raw:    hex.EncodeToString(ver.Marshal()),
		Change: changeUtxo,
	}
	return transaction, nil
}

func DecodeRawTx(raw string, _ int) (string, error) {
	rawBytes, err := hex.DecodeString(raw)
	if err != nil {
		return "", err
	}
	ver, err := common.UnmarshalVersionedTransaction(rawBytes)
	if err != nil {
		return "", err
	}
	tx, err := json.Marshal(ver)
	if err != nil {
		return "", err
	}
	return string(tx), nil
}
