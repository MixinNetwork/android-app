#!/bin/bash

mnm run './gradlew clean :app:bundleRelease'
fastlane supply --aab ./app/build/outputs/bundle/release/app-release.aab --track internal


