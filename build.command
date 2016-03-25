#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR && ./gradlew assemble :client:jfxNative && open $DIR/client/build/jfx/native/
