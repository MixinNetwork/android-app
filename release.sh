#!/bin/bash

mnm run './gradlew clean :app:bundleRelease'
fastlane supply --aab ./app/build/outputs/bundle/googlePlayRelease/app-googlePlay-release.aab --track internal


