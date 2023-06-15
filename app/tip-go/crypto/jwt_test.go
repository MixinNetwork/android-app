package crypto

import (
	"encoding/hex"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestParseIat(t *testing.T) {
	token := "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2ODY3NTYxMjQsImlhdCI6MTY4Njc1NDMyNCwianRpIjoiYjg0Y2M1MDMtNzQ3MC00ZDg5LThkOTAtN2NiMzNlNTk0MzdkIiwic2NwIjoiRlVMTCIsInNpZCI6Ijk0ZWY2YTdmLTUyYTYtNDAxOS05OTZmLWMzMGM3N2YyNDhhNiIsInNpZyI6IjVlNmI1OGZmYTEwYjNiYzUxNzI0ZmYwYmJkMmFmYjkxYzQ3NzFlZTM0MGY1ZDY4NTM0MGRmYTRjODU0YmFmYmEiLCJ1aWQiOiIzYjAzNTc3Ni05NjNkLTQ5ZWQtODUwYy0yMmVjMzA5NjU0ODEifQ.d39RpuAmHyiSVQjKsWg7MxcaLT0kj8GUw53oQZzzUwr0fwUXYawKPWdpjc5GhQqaxpiX2PkVYxGgWxaB2RPRCw"
	pub, _ := hex.DecodeString("0b6ab2fa8683d01c530e80c219857a0e9e3abec30858a8da3b073de01d33058a")
	iat := ParseIat(token, pub)
	assert.True(t, iat == -1)
	t.Logf("sig %d", iat)

	token = "eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2ODY4MTg4NzEsImlhdCI6MTY4NjgxNzA3MSwianRpIjoiZTAxY2ZiNGMtNTg3YS00NjgwLWExNDktMDY0MDRlNzhkNzhkIiwic2NwIjoiRlVMTCIsInNpZCI6Ijk0ZWY2YTdmLTUyYTYtNDAxOS05OTZmLWMzMGM3N2YyNDhhNiIsInNpZyI6IjVlNmI1OGZmYTEwYjNiYzUxNzI0ZmYwYmJkMmFmYjkxYzQ3NzFlZTM0MGY1ZDY4NTM0MGRmYTRjODU0YmFmYmEiLCJ1aWQiOiIzYjAzNTc3Ni05NjNkLTQ5ZWQtODUwYy0yMmVjMzA5NjU0ODEifQ.GEGe3AvkHQLYOvePekKQJHlP2Sp-iliMw8a1oyUZeDutPuk0XX77Q852tBZEPQD-Iao4W7a3PP9VygLFzkVtJynQaF3mDyjWo4HjjKZpFOztQy7Rme72GT3zPOlFbxSwfjNXvvggQfodQPGcA6JPZKv4emWkZKNjL6EC4fWF6Z4"
	public := "-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCfI6GY062Drru/owA5tnSAVZ7vsNoB0Dqe23JMC37qj5o/385qUy6IfrNRPwHKbdyTr1QubrvlXGjMVEkBU39n0rYbmWLxfx7X4OBksGRcMfk69V73aHVidTjBYW0Rl/7S6HGUbar6iyRtosyydlDjwBRtN8dsL/v+QGFUepuBeQIDAQAB\n-----END PUBLIC KEY-----"
	iat = ParseIat(token, []byte(public))
	assert.True(t, iat == -1)
	t.Logf("sig %d", iat)
}
