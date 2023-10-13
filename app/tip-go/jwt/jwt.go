package jwt

import (
	"crypto/ed25519"
	"errors"
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

func SignToken(requestId, uid, sid, sig string, privateKey []byte) (string, error) {
	expire := time.Now().UTC().Add(time.Second * 60 * 30)

	claims := jwt.MapClaims{
		"jti": requestId,
		"iat": time.Now().UTC().Unix(),
		"exp": expire.Unix(),
		"uid": uid,
		"sid": sid,
		"sig": sig,
		"scp": "FULL",
	}
	if len(privateKey) > 64 {
		key, err := jwt.ParseRSAPrivateKeyFromPEM(privateKey)
		if err != nil {
			return "", err
		}
		token := jwt.NewWithClaims(jwt.SigningMethodRS512, claims)
		return token.SignedString(key)
	}

	if len(privateKey) != 64 {
		return "", fmt.Errorf("bad ed25519 private key %s", privateKey)
	}
	token := jwt.NewWithClaims(jwt.SigningMethodEdDSA, claims)
	return token.SignedString(ed25519.PrivateKey(privateKey))
}

func ParseIat(tokenString string, verifyKey []byte) int64 {
	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		if len(verifyKey) == 32 {
			return ed25519.PublicKey(verifyKey), nil
		} else {
			key, err := jwt.ParseRSAPublicKeyFromPEM(verifyKey)
			if err != nil {
				return "", err
			}
			return key, nil
		}
	})
	if err != nil {
		if errors.Is(err, jwt.ErrTokenExpired) {
			return -1
		}
		return 0
	}
	if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
		return int64(claims["iat"].(float64))
	} else {
		return 0
	}
}
