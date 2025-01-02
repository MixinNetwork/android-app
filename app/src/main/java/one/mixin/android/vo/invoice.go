package bot

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"strings"

	"github.com/MixinNetwork/mixin/common"
	"github.com/MixinNetwork/mixin/crypto"
	"github.com/gofrs/uuid/v5"
)

const (
	MixinInvoiceVersion = byte(0)
	MixinInvoicePrefix  = "MIN"
)

type MixinInvoice struct {
	version   byte
	Recipient *MixAddress
	Entries   []*InvoiceEntry
}

type InvoiceEntry struct {
	TraceId         uuid.UUID
	AssetId         uuid.UUID
	Amount          common.Integer
	Extra           []byte
	IndexReferences []byte
	HashReferences  []crypto.Hash
}

func NewMixinInvoice(recipient string) *MixinInvoice {
	r, err := NewMixAddressFromString(recipient)
	if err != nil {
		panic(recipient)
	}
	mi := &MixinInvoice{
		version:   MixinInvoiceVersion,
		Recipient: r,
	}
	return mi
}

func (mi *MixinInvoice) AddEntry(traceId, assetId string, amount common.Integer, extra []byte, indexReferences []byte, externalReferences []crypto.Hash) {
	e := &InvoiceEntry{
		TraceId:        uuid.Must(uuid.FromString(traceId)),
		AssetId:        uuid.Must(uuid.FromString(assetId)),
		Amount:         amount,
		Extra:          extra,
		HashReferences: externalReferences,
	}
	if len(e.HashReferences)+len(indexReferences) > common.ReferencesCountLimit {
		panic("too many references")
	}
	for _, ir := range indexReferences {
		if int(ir) >= len(mi.Entries) {
			panic(len(mi.Entries))
		}
		e.IndexReferences = append(e.IndexReferences, ir)
	}
	mi.Entries = append(mi.Entries, e)
}

func (mi *MixinInvoice) BytesUnchecked() []byte {
	enc := common.NewEncoder()
	enc.Write([]byte{mi.version})
	rb := mi.Recipient.BytesUnchecked()
	if len(rb) > 1024 {
		panic(len(rb))
	}
	enc.WriteUint16(uint16(len(rb)))
	enc.Write(rb)

	if len(mi.Entries) > 128 {
		panic(len(mi.Entries))
	}
	enc.Write([]byte{byte(len(mi.Entries))})

	for _, e := range mi.Entries {
		enc.Write(e.TraceId.Bytes())
		enc.Write(e.AssetId.Bytes())
		ab := len(e.Amount.String())
		if ab > 128 {
			panic(e.Amount.String())
		}
		enc.Write([]byte{byte(len(e.Amount.String()))})
		enc.Write([]byte(e.Amount.String()))
		if len(e.Extra) >= common.ExtraSizeStorageCapacity {
			panic(len(e.Extra))
		}
		enc.WriteInt(len(e.Extra))
		enc.Write(e.Extra)

		rl := len(e.IndexReferences) + len(e.HashReferences)
		if rl > common.ReferencesCountLimit {
			panic(rl)
		}
		enc.Write([]byte{byte(rl)})
		for _, ir := range e.IndexReferences {
			enc.Write([]byte{1, ir})
		}
		for _, er := range e.HashReferences {
			enc.Write([]byte{0})
			enc.Write(er[:])
		}
	}

	return enc.Bytes()
}

func (mi *MixinInvoice) String() string {
	payload := mi.BytesUnchecked()
	data := append([]byte(MixinInvoicePrefix), payload...)
	checksum := crypto.Sha256Hash(data)
	payload = append(payload, checksum[:4]...)
	return MixinInvoicePrefix + base64.RawURLEncoding.EncodeToString(payload)
}

func NewMixinInvoiceFromString(s string) (*MixinInvoice, error) {
	var mi MixinInvoice
	if !strings.HasPrefix(s, MixinInvoicePrefix) {
		return nil, fmt.Errorf("invalid invoice prefix %s", s)
	}
	data, err := base64.RawURLEncoding.DecodeString(s[len(MixinInvoicePrefix):])
	if err != nil {
		return nil, fmt.Errorf("invalid invoice base64 %v", err)
	}
	if len(data) < 3+23+1 {
		return nil, fmt.Errorf("invalid invoice length %d", len(data))
	}
	payload := data[:len(data)-4]
	checksum := crypto.Sha256Hash(append([]byte(MixinInvoicePrefix), payload...))
	if !bytes.Equal(checksum[:4], data[len(data)-4:]) {
		return nil, fmt.Errorf("invalid invoice checksum %x", checksum[:4])
	}

	dec := common.NewDecoder(payload)
	mi.version, err = dec.ReadByte()
	if err != nil || mi.version != MixinInvoiceVersion {
		return nil, fmt.Errorf("invalid invoice version %d %v", mi.version, err)
	}
	rbl, err := dec.ReadUint16()
	if err != nil {
		return nil, err
	}
	rb := make([]byte, rbl)
	err = dec.Read(rb)
	if err != nil {
		return nil, err
	}
	mi.Recipient, err = NewMixAddressFromBytesUnchecked(rb)
	if err != nil {
		return nil, err
	}

	el, err := dec.ReadByte()
	if err != nil {
		return nil, err
	}
	for ; el > 0; el-- {
		var e InvoiceEntry
		b := make([]byte, 16)
		err = dec.Read(b)
		if err != nil {
			return nil, err
		}
		e.TraceId = uuid.Must(uuid.FromBytes(b))
		err = dec.Read(b)
		if err != nil {
			return nil, err
		}
		e.AssetId = uuid.Must(uuid.FromBytes(b))

		al, err := dec.ReadByte()
		if err != nil {
			return nil, err
		}
		b = make([]byte, al)
		err = dec.Read(b)
		if err != nil {
			return nil, err
		}
		e.Amount = common.NewIntegerFromString(string(b))

		e.Extra, err = dec.ReadBytes()
		if err != nil {
			return nil, err
		}

		rl, err := dec.ReadByte()
		if err != nil {
			return nil, err
		}
		if rl > common.ReferencesCountLimit {
			return nil, fmt.Errorf("too many references %d", rl)
		}
		for ; rl > 0; rl-- {
			rv, err := dec.ReadByte()
			if err != nil {
				return nil, err
			}
			switch rv {
			case 0:
				var b crypto.Hash
				err = dec.Read(b[:])
				if err != nil {
					return nil, err
				}
				e.HashReferences = append(e.HashReferences, b)
			case 1:
				ir, err := dec.ReadByte()
				if err != nil {
					return nil, err
				}
				if int(ir) >= len(mi.Entries) {
					return nil, fmt.Errorf("invalid reference index %d", ir)
				}
				e.IndexReferences = append(e.IndexReferences, ir)
			default:
				return nil, fmt.Errorf("invalid reference type %d", rv)
			}
		}
		mi.Entries = append(mi.Entries, &e)
	}
	return &mi, nil
}