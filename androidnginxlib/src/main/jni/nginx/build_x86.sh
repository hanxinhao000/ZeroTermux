#!/bin/bash
$ANDROID_NDK_HOME/build/tools/make-standalone-toolchain.sh --platform=android-19 --install-dir=$HOME/local/android-toolchain --toolchain=x86-4.9 --force --verbose
auto/configure --crossbuild=android-x86 --prefix=/sdcard/nginx --with-cc=$HOME/local/android-toolchain/bin/i686-linux-android-gcc --without-pcre --without-http_userid_module --without-http_rewrite_module --with-cc-opt=-Wno-sign-compare
make -j8
