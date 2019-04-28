#!/bin/bash
set -e -u

## Install additional packages.
sudo DEBIAN_FRONTEND=noninteractive \
    apt-get install -yq --no-install-recommends \
    automake bison ca-certificates curl flex g++ git gettext \
    gperf libglib2.0-dev libtool-bin m4 pkg-config python \
    python3.7 tar texinfo unzip xmlto zip

sudo mkdir -p "/data/data/xeffyr.alpine.term/files/environment"
sudo chown -R "$(whoami)" /data

## Install Android NDK.
. $(cd "$(dirname "$0")"; pwd)/properties.sh
ANDROID_NDK_FILE=android-ndk-r${TERMUX_NDK_VERSION}-Linux-x86_64.zip
ANDROID_NDK_SHA256=0fbb1645d0f1de4dde90a4ff79ca5ec4899c835e729d692f433fda501623257a

if [ ! -d $NDK ]; then
    mkdir -p $NDK
    cd $NDK/..
    rm -Rf `basename $NDK`
    echo "Downloading android ndk..."
    curl --fail --retry 3 -o ndk.zip \
        https://dl.google.com/android/repository/${ANDROID_NDK_FILE}
    echo "${ANDROID_NDK_SHA256} ndk.zip" | sha256sum -c -
    rm -Rf android-ndk-r$TERMUX_NDK_VERSION
    unzip -q ndk.zip
    mv android-ndk-r$TERMUX_NDK_VERSION `basename $NDK`
    rm ndk.zip
fi
