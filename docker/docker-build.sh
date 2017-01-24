#!/bin/sh

set -e # exit on error

DOCKER_IMAGE="twyatt/galactic-aztec-heavy-data-acquisition:latest"

set -x # echo on

docker build -t ${DOCKER_IMAGE} .