#!/bin/bash

gomobile bind -o ../libs/mixin.aar -target=android/arm,android/arm64 mixin/tip mixin/jwt mixin/ed25519
rm ../libs/mixin-sources.jar
