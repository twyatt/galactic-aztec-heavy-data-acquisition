# Galactic Aztec Heavy Data Acquisition Software
[![Build Status](https://travis-ci.org/twyatt/galactic-aztec-heavy-data-acquisition.svg?branch=phidgets)](https://travis-ci.org/twyatt/galactic-aztec-heavy-data-acquisition)


This project is a simplified fork of the original [Galactic Aztec Data Acquisition] project, designed to be used with the [Phidgets Bridge] board.

The software suite consists of a server (designed to run on a Raspberry Pi) and a multi-platform client application. The server reads from the [Phidgets Bridge] and transmits the data to the client application which displays the data in a gauge and graph.

Designed for the [SDSU Rocket Project] to be used for the [Galactic Aztec Heavy] rocket in order to read a load cell during the static test. Although it can operate as a general purpose data acquisition software suite, providing data recording and transmission capabilities.


## Server

### Prerequisites

The server requires that the [Phidgets drivers] be installed, which can be done by executing the following commands on your Raspberry Pi:
```
sudo apt-get update
sudo apt-get install libusb-1.0-0-dev
wget -qO- http://www.phidgets.com/downloads/libraries/libphidget.tar.gz | tar xv
cd libphidget*
./configure
make
sudo make install
```

### Getting Started

Browse the [releases page] to determine the latest version, then SSH into your Raspberry Pi and run a command similar to:
```
wget -qO- https://github.com/twyatt/galactic-aztec-heavy-data-acquisition/releases/download/1.1.0-phidgets/server-phidgets.tar | tar xv
```
_Where `1.1.0-phidgets` should be replaced with the latest version number appearing on the [latest release] page._

The server is designed to record all readings to file; you can specify the directories to save the recordings to when you start the server:
```
sudo server-phidgets/bin/server logs/
```
_The server can be executed with the `--help` argument to see more options._

Alternatively, if you wish to start the server without recording readings, then you can use the `--allow-no-logs` argument:
```
sudo server-phidgets/bin/server --allow-no-logs
```

### Usage

After the server has been started, you can issue commands to perform functions such as printing out readings or quiting the application. To see a list of available options press `?` followed by the `Enter` key. To quit, press `q` then `Enter`.

### Testing

If you don't have the [Galactic Aztec Heavy Raspberry Pi Add-on: ADC], or similar board available, then you can start the server in testing mode (whereas it generates sinusoidal test data):
```
server-phidgets/bin/server --test
```

### Logging

The server logs to a simple binary format that can be converted to CSV using `log2csv`, which can be installed by running a command similar to:
```
wget -qO- https://github.com/twyatt/galactic-aztec-heavy-data-acquisition/releases/download/1.1.0/log2csv-phidgets.tar | tar xv
```
_Where `1.1.0` should be replaced with the latest version number appearing on the [latest release] page._

You can then convert logs produced by the server by running `log2csv` with the log directory as the only argument, for example:
```
log2csv-phidgets/bin/log2csv server-phidgets/bin/logs/20161113233302
```
In this example, we are running `log2csv` and specifying a log directory that was created after running the server; be sure and replace `20161113233302` from the example with the appropriate directory created after running the server.

The client does not write sensor config data, so `log2csv` must be instructed to skip the config data when converting logs written by the client, for example:
```
log2csv-phidgets/bin/log2csv --skip-config server-phidgets/bin/logs/20161113233302
```


## Client

[![Client Gauges Screenshot](artwork/thumb_client.png?raw=true)](artwork/client.png?raw=true)

### OS X

Simply download and install the OS X package (named `DataAcquisitionClientPhidgets-VERSION.dmg`) from the [latest release] page.
_Where `VERSION` represents the latest version number appearing on the [latest release] page._

### Windows

Locate and download the latest `DataAcquisitionClientPhidgets.zip` file from the [releases page]. Extract and run `DataAcquisitionClientPhidgets.exe`.

### Linux

Locate and download the latest `dataacquisitionclient-phidgets-VERSION.deb` file from the [latest release].
_Where `VERSION` represents the latest version number appearing on the [latest release] page._

On Debian based systems, you can install the package by running:

```
sudo dpkg -i dataacquisitionclient-phidgets-*.deb && sudo apt-get install -f
```

### Building From Source

_If you don't already have [Java SE Development Kit 8] installed, download and install it._

#### OS X

1. Download and extract the [latest Source code (zip)].
1. Open the `build.command` script.

The built application will be saved into the `client/build/jfx/native/` sub-directory.

#### Windows

1. Download and extract the [latest Source code (zip)].
1. Open the `build.bat` script.

The built application will be saved into the `client/build/jfx/native/` sub-directory.

#### Linux

The Linux distribution can be built on a Linux machine or via Docker image.

To build on a Linux machine you'll need:

- JDK 8 (Oracle or OpenJDK w/ JavaFX)
- fakeroot
- rpmbuild

On Debian based systems you can run:

```
sudo apt-get update && sudo apt-get install fakeroot rpm
./gradlew :client:jfxNative
```

Alternatively, if you have [Docker] installed, then you can simply run:

```
docker/project-build.sh
```


[Galactic Aztec Data Acquisition]: https://github.com/twyatt/galactic-aztec-data-acquisition
[Phidgets Bridge]: http://www.phidgets.com/products.php?product_id=1046
[SDSU Rocket Project]: http://rocket.sdsu.edu/
[Galactic Aztec Heavy]: http://rocket.sdsu.edu/rockets#galactic-aztec-heavy
[Phidgets drivers]: http://www.phidgets.com/docs/OS_-_Linux#Installing
[releases page]: https://github.com/twyatt/galactic-aztec-heavy-data-acquisition/releases
[Java SE Development Kit 8]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[latest Source code (zip)]: https://github.com/twyatt/galactic-aztec-heavy-data-acquisition/zipball/master
[Docker]: https://www.docker.com