#!/bin/bash

gomobile bind -o ../libs/tip.aar -target=android/arm,android/arm64 tip/crypto tip/abi
rm ../libs/tip-sources.jar
