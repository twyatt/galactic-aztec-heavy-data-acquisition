FROM openjdk:8-jdk
MAINTAINER Travis Wyatt <travis.i.wyatt@gmail.com>

RUN apt-get update \
  && apt-get install -y --no-install-recommends openjfx fakeroot rpm \
  && rm -rf /var/lib/apt/lists/*