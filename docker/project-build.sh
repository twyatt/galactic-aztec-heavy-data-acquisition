#!/bin/sh

set -e # exit on error

DOCKER_IMAGE="twyatt/galactic-aztec-heavy-data-acquisition:latest"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT=$( dirname ${DIR} )

ARG='cd /project && ./gradlew -Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Dorg.gradle.jvmargs= clean assemble :client:jfxNative'

set -x # echo on

docker run -it --rm -v "${PROJECT}":/project ${DOCKER_IMAGE} /bin/bash -c "${ARG}"