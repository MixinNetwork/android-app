#!/bin/bash
gomobile bind -target=ios,iossimulator/arm64 -o ./tip.xcframework -trimpath -ldflags "-s -w" mixin/tip mixin/kernel
