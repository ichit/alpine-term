# Build with:
#  docker build -t xeffyr/alpine-term-env-builder .
# Push to docker hub with:
#  docker push xeffyr/alpine-term-env-builder
# This is done after changing this file or any of the
# scripts/setup-{ubuntu,android-sdk}.sh setup scripts.
FROM ubuntu:18.10

# Fix locale to avoid warnings:
ENV LANG C.UTF-8

# Needed for setup:
COPY ./setup-build-environment.sh /tmp/setup-build-environment.sh
COPY ./properties.sh /tmp/properties.sh

# Setup needed packages and the Android SDK and NDK:
RUN apt-get update && \
    apt-get -yq upgrade && \
    apt-get install -yq sudo && \
    adduser --disabled-password --shell /bin/bash --gecos "" builder && \
    echo "builder ALL=(root) NOPASSWD:ALL" > /etc/sudoers.d/builder && \
    chmod 0440 /etc/sudoers.d/builder && \
    su - builder -c /tmp/setup-build-environment.sh && \
    # Removed unused parts to make a smaller Docker image:
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    cd /home/builder/lib/android-ndk/ && \
    rm -Rf toolchains/mips* && \
    rm -Rf sources/cxx-stl/gabi++ sources/cxx-stl/system sources/cxx-stl/stlport sources/cxx-stl/gnu-libstdc++

# We expect this to be mounted with '-v $PWD:/home/builder/env-packages':
WORKDIR /home/builder/env-packages
