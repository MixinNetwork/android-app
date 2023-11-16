package kernel

import "github.com/MixinNetwork/mixin/common"

type Address struct {
	address common.Address
}

func NewMainAddressFromString(s string) (*Address, error) {
	a, err := common.NewAddressFromString(s)
	return &Address{a}, err
}

func (a *Address) PublicSpendKey() []byte {
	return a.address.PublicSpendKey[:]
}

func (a *Address) PublicViewkey() []byte {
	return a.address.PublicViewKey[:]
}

func (a *Address) SetPublicSpendKey(k []byte) {
	copy(a.address.PublicSpendKey[:], k[:])
}

func (a *Address) SetPublicViewKey(k []byte) {
	copy(a.address.PublicViewKey[:], k[:])
}

func (a *Address) String() string {
	return a.address.String()
}
