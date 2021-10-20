FROM gradle:7.1-jdk11

ENV ANDROID_SDK_URL https://dl.google.com/android/repository/commandlinetools-linux-7302050_latest.zip
ENV ANDROID_API_LEVEL android-31
ENV ANDROID_BUILD_TOOLS_VERSION 31.0.0
ENV ANDROID_HOME /usr/local/android-sdk-linux
ENV ANDROID_NDK_VERSION 23.0.7599858
ENV ANDROID_VERSION 31
ENV ANDROID_NDK_HOME ${ANDROID_HOME}/ndk/${ANDROID_NDK_VERSION}/
ENV PATH ${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools

RUN mkdir "$ANDROID_HOME" .android && \
    cd "$ANDROID_HOME" && \
    curl -o sdk.zip $ANDROID_SDK_URL && \
    unzip sdk.zip && \
    rm sdk.zip

RUN yes | ${ANDROID_HOME}/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses
RUN $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --update
RUN $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME "build-tools;30.0.3" \
    "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "platforms;android-${ANDROID_VERSION}" \
    "platform-tools" \
    "ndk;$ANDROID_NDK_VERSION"
RUN cp $ANDROID_HOME/build-tools/30.0.3/dx $ANDROID_HOME/build-tools/31.0.0/dx
RUN cp $ANDROID_HOME/build-tools/30.0.3/lib/dx.jar $ANDROID_HOME/build-tools/31.0.0/lib/dx.jar
ENV PATH ${ANDROID_NDK_HOME}:$PATH
ENV PATH ${ANDROID_NDK_HOME}/prebuilt/linux-x86_64/bin/:$PATH

CMD mkdir -p /home/source/apk && \
    cp -R /home/source/. /home/gradle && \
    cd /home/gradle && \
    ./gradlew assembleRelease --no-daemon -Dkotlin.compiler.execution.strategy="in-process" && \
    cp -R /home/gradle/app/build/outputs/apk/release/*.apk /home/source/apk/mixin-android.apk