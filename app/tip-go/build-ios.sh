#!/bin/bash

# go install github.com/mixinnetwork/mobile/cmd/gomobile
# go install github.com/mixinnetwork/mobile/cmd/gobind

gomobile bind -target=ios,iossimulator/arm64 -o ./tip.xcframework -trimpath -ldflags "-s -w" mixin/tip mixin/kernel
