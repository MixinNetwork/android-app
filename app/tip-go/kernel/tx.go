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

func BuildTxToKernelAddress(asset string, amount string, kenelAddress string, inputs []byte, changeKeys, changeMask, extra string) (string, error) {
	a, err := common.NewAddressFromString(kenelAddress)
	if err != nil {
		return "", err
	}
	seed := make([]byte, 64)
	crypto.ReadRand(seed)
	r := crypto.NewKeyFromSeed(seed)
	receiverMask := r.Public()
	keys := crypto.DeriveGhostPublicKey(&r, &a.PublicViewKey, &a.PublicSpendKey, uint64(0))

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
	err = json.Unmarshal(inputs, &utxo)
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
				Index: uint(ut.Index),
			},
			Output: common.Output{
				Amount: amount,
			},
		}
		ins = append(ins, &u)
	}

	return buildTransaction(asset, amount, 1, []*crypto.Key{keys}, receiverMask, ins, cks, changeMask, extra, "")
}

func BuildTx(asset string, amount string, threshold uint8, receiverKeys string, receiverMask string, inputs []byte, changeKeys, changeMask, extra, reference string) (string, error) {
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
				Index: uint(ut.Index),
			},
			Output: common.Output{
				Amount: amount,
			},
		}
		ins = append(ins, &u)
	}
	mask, err := crypto.KeyFromString(receiverMask)
	if err != nil {
		return "", err
	}
	return buildTransaction(asset, amount, threshold, rks, mask, ins, cks, changeMask, extra, reference)
}

func BuildWithdrawalTx(asset string, amount, address, tag string, feeAmount, feeKeys string, feeMask string, inputs []byte, changeKeys, changeMask, extra string) (*Tx, error) {
	rks := []*crypto.Key{}
	if feeKeys != "" {
		keys := strings.Split(feeKeys, ",")
		for _, k := range keys {
			key := k
			rk, err := crypto.KeyFromString(key)
			if err != nil {
				return nil, err
			}
			rks = append(rks, &rk)
		}
	}
	cks := []*crypto.Key{}
	if changeKeys != "" {
		ckeys := strings.Split(changeKeys, ",")
		for _, k := range ckeys {
			ke := k
			rk, err := crypto.KeyFromString(ke)
			if err != nil {
				return nil, err
			}
			cks = append(cks, &rk)
		}
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
			return nil, err
		}
		amount := common.NewIntegerFromString(ut.Amount)
		u := common.UTXO{
			Input: common.Input{
				Hash:  h,
				Index: uint(ut.Index),
			},
			Output: common.Output{
				Amount: amount,
			},
		}
		ins = append(ins, &u)
	}
	return buildWithrawalTransaction(asset, amount, ins, address, tag, feeAmount, rks, feeMask, cks, changeMask, extra)
}

func buildWithrawalTransaction(asset, amount string, inputs []*common.UTXO, address, tag string, feeAmount string, feeKeys []*crypto.Key, feeMask string, changeKeys []*crypto.Key, changeMask string, extra string) (*Tx, error) {
	assetHash, err := crypto.HashFromString(asset)
	if err != nil {
		return nil, err
	}

	amountValue := common.NewIntegerFromString(amount)
	feeAmountValue := common.NewInteger(0)
	if feeAmount != "" {
		feeAmountValue = common.NewIntegerFromString(feeAmount)
	}
	total := common.NewInteger(0)

	tx := common.NewTransactionV5(assetHash)
	for _, in := range inputs {
		tx.AddInput(in.Hash, in.Index)
		total = total.Add(in.Amount)
	}
	if feeAmountValue.Cmp(common.Zero) > 0 && total.Cmp(amountValue.Add(feeAmountValue)) < 0 {
		return nil, errors.New("insufficient funds")
	}
	withdrawalOutput := &common.Output{
		Type:   common.OutputTypeWithdrawalSubmit,
		Amount: amountValue,
		Withdrawal: &common.WithdrawalData{
			Address: address,
			Tag:     tag,
		},
	}
	tx.Outputs = append(tx.Outputs, withdrawalOutput)
	if feeAmount != "" {
		if feeMask == "" {
			return nil, errors.New("bad param address")
		}
		mask, err := crypto.KeyFromString(feeMask)
		if err != nil {
			return nil, err
		}
		if !mask.CheckKey() {
			return nil, errors.New("invalid mask")
		}

		feeOutput := &common.Output{
			Type:   common.OutputTypeScript,
			Amount: feeAmountValue,
			Keys:   feeKeys,
			Mask:   mask,
			Script: common.NewThresholdScript(1),
		}
		tx.Outputs = append(tx.Outputs, feeOutput)
	}

	amountAndFee := amountValue
	if feeAmount != "" {
		amountAndFee = amountAndFee.Add(feeAmountValue)
	}
	if total.Cmp(amountAndFee) > 0 {
		change := total.Sub(amountAndFee)
		script := common.NewThresholdScript(1)

		changeMaskKey, err := crypto.KeyFromString(changeMask)
		if err != nil {
			return nil, err
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
			return nil, errors.New("extra data is too long")
		}
		tx.Extra = extraBytes
	}

	ver := tx.AsVersioned()
	t := &Tx{
		Raw:  hex.EncodeToString(ver.Marshal()),
		Hash: ver.PayloadHash().String(),
	}
	return t, nil
}

func buildTransaction(asset string, amount string, threshold uint8, receiverKeys []*crypto.Key, receiverMask crypto.Key, inputs []*common.UTXO, changeKeys []*crypto.Key, changeMask string, extra, reference string) (string, error) {
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

	if !receiverMask.CheckKey() {
		return "", errors.New("invalid mask")
	}
	output := &common.Output{
		Type:   common.OutputTypeScript,
		Amount: amountValue,
		Keys:   receiverKeys,
		Mask:   receiverMask,
		Script: common.NewThresholdScript(threshold),
	}
	tx.Outputs = append(tx.Outputs, output)

	if reference != "" {
		h, err := crypto.HashFromString(reference)
		if err != nil {
			return "", errors.New("bad param reference")
		}
		tx.References = append(tx.References, h)
	}
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

func SignTx(raw, inputKeys, viewKeys string, spendKey string, withoutFee bool) (*Tx, error) {
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
			return nil, fmt.Errorf("invalid public key for the input %s, %s", input, key.Public().String())
		}
		sig := key.Sign(msg)
		sigs := make(map[uint16]*crypto.Signature)
		sigs[i] = &sig
		ver.SignaturesMap = append(ver.SignaturesMap, sigs)
	}
	var changeUtxo *Utxo
	if ver.Outputs[0].Withdrawal != nil {
		if len(ver.Outputs) == 3 {
			changeIndex := len(ver.Outputs) - 1
			changeUtxo = &Utxo{
				Hash:   ver.PayloadHash().String(),
				Amount: ver.Outputs[changeIndex].Amount.String(),
				Index:  changeIndex,
			}
		} else if len(ver.Outputs) == 2 {
			if withoutFee {
				changeIndex := len(ver.Outputs) - 1
				changeUtxo = &Utxo{
					Hash:   ver.PayloadHash().String(),
					Amount: ver.Outputs[changeIndex].Amount.String(),
					Index:  changeIndex,
				}
			}
		}
	} else {
		if len(ver.Outputs) > 1 {
			changeIndex := len(ver.Outputs) - 1
			changeUtxo = &Utxo{
				Hash:   ver.PayloadHash().String(),
				Amount: ver.Outputs[changeIndex].Amount.String(),
				Index:  changeIndex,
			}
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
