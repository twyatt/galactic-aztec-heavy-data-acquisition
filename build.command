#!/bin/bash

set -e # exit on error

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

set -x # echo on

cd $DIR

# disable daemon to prevent issue #12 (https://github.com/FibreFoX/javafx-gradle-plugin/issues/12)
./gradlew \
  -Dorg.gradle.daemon=false \
  -Dorg.gradle.parallel=false \
  -Dorg.gradle.jvmargs= \
  clean \
  assemble :client:jfxNative

open $DIR/client/build/jfx/native/
