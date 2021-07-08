#!/bin/bash
$ANDROID_NDK_HOME/build/tools/make-standalone-toolchain.sh --platform=android-21 --install-dir=$HOME/local/android-toolchain --toolchain=arm-linux-androideabi-4.9 --force --verbose
auto/configure --crossbuild=android-arm --prefix=/sdcard/nginx --with-cc=$HOME/local/android-toolchain/bin/arm-linux-androideabi-gcc --without-pcre --without-http_userid_module --without-http_rewrite_module --with-cc-opt=-Wno-sign-compare
make -j8
