#!/bin/bash

# go install github.com/mixinnetwork/mobile/cmd/gomobile
# go install github.com/mixinnetwork/mobile/cmd/gobind

gomobile bind -o ../libs/mixin.aar -trimpath -ldflags "-s -w" -target=android/arm,android/arm64 mixin/tip mixin/jwt mixin/ed25519 mixin/kernel
rm ../libs/mixin-sources.jar
