# Galactic Aztec Heavy Data Acquisition Software
[![Build Status](https://travis-ci.org/twyatt/galactic-aztec-heavy-data-acquisition.svg?branch=master)](https://travis-ci.org/twyatt/galactic-aztec-heavy-data-acquisition)

Server and client application suite for reading sensors from a Raspberry Pi and transmitting to a remote client. This is a fork of the original [Galactic Aztec Data Acquisition Software].

## Server
### Installation
```
./gradlew :server:distZip
scp server/build/distributions/server.zip pi@raspberrypi:~
```

SSH into your Raspberry Pi:
```
ssh pi@raspberrypi
```

Then the server can be extracted/started with the following commands:
```
unzip server.zip
server/bin/server
```

## Client
### Run

The client application can either be started via command line or IntelliJ.

#### Command Line
```
./gradlew :client:run
```

#### IntelliJ
Run `Launcher.main()`:

![Run Launcher](artwork/client_launch.png?raw=true)


[Galactic Aztec Data Acquisition Software]: https://github.com/twyatt/galactic-aztec-data-acquisition
