#!/bin/sh

set -e # exit on error

DOCKER_IMAGE="twyatt/galactic-aztec-heavy-data-acquisition:latest"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT=$( dirname ${DIR} )

set -x # echo on

docker run \
  -it \
  --rm \
  -v "${PROJECT}":/project \
  -w '/project' \
  ${DOCKER_IMAGE} \
  /bin/bash