#!/bin/bash

gomobile bind -o ../libs/tip.aar -target=android/arm,android/arm64 tip/crypto
rm ../libs/tip-sources.jar
