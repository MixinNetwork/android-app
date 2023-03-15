package abi

import (
	"encoding/hex"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/ethereum/go-ethereum/accounts/abi"
)

type param struct {
	Name  string
	Value string
	Type  string
}
type methodData struct {
	Name   string
	Params []param
}

func DecodeMethod(abiJSON, txData string) (string, error) {
	contractABI, err := abi.JSON(strings.NewReader(abiJSON))
	if err != nil {
		return "", err
	}

	txData = strings.TrimPrefix(txData, "0x")
	decodedSig, err := hex.DecodeString(txData[:8])
	if err != nil {
		return "", err
	}

	method, err := contractABI.MethodById(decodedSig)
	if err != nil {
		return "", err
	}

	decodedData, err := hex.DecodeString(txData[8:])
	if err != nil {
		return "", err
	}

	inputs, err := method.Inputs.Unpack(decodedData)
	if err != nil {
		return "", err
	}

	nonIndexedArgs := method.Inputs.NonIndexed()
	methodData := methodData{}
	methodData.Name = method.Name
	for i, input := range inputs {
		arg := nonIndexedArgs[i]
		param := param{
			Name:  arg.Name,
			Value: fmt.Sprintf("%v", input),
			Type:  arg.Type.String(),
		}
		methodData.Params = append(methodData.Params, param)
	}

	b, err := json.Marshal(methodData)
	if err != nil {
		return "", err
	}
	return string(b), nil
}

// type swapDescription struct {
// 	SrcToken         common.Address
// 	DstToken         common.Address
// 	SrcReceiver      common.Address
// 	DstReceiver      common.Address
// 	Amount           *big.Int
// 	MinReturnAmount  *big.Int
// 	GuaranteedAmount *big.Int
// 	Flags            *big.Int
// }
