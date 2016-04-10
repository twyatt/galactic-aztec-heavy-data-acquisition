# Galactic Aztec Heavy Data Acquisition Software
[![Build Status](https://travis-ci.org/twyatt/galactic-aztec-heavy-data-acquisition.svg?branch=master)](https://travis-ci.org/twyatt/galactic-aztec-heavy-data-acquisition)


This project is a simplified fork of the original [Galactic Aztec Data Acquisition] project, designed to be used with the [Galactic Aztec Heavy Raspberry Pi Add-on: ADC] board.

The software suite consists of a server (designed to run on a Raspberry Pi) and a multi-platform client application. The server reads from the Analog-to-Digital converters and transmits the data to the client application which displays the data in gauges and graphs.

Designed for the [SDSU Rocket Project] to be used on the [Galactic Aztec Heavy] rocket in order to read tank pressures. Although it can operate as a general purpose data acquisition software suite, providing data recording and transmission capabilities.


## Server

### Getting Started

Browse the [latest release] page to determine the latest version, then SSH into your Raspberry Pi and run a command similar to:
```
wget -qO- https://github.com/twyatt/galactic-aztec-heavy-data-acquisition/releases/download/1.0.0/server.tar | tar xv
```
_Where `1.0.0` should be replaced with the latest version number appearing on the [latest release] page._

The server is designed to record all readings to file; you can specify the directories to save the recordings to when you start the server:
```
server/bin/server logs/
```
_The server can be executed with the `--help` argument to see more options._

Alternatively, if you wish to start the server without recording readings, then you can use the `--allow-no-logs` argument:
```
server/bin/server --allow-no-logs
```

### Usage

After the server has been started, you can issue commands to perform functions such as printing out readings or quiting the application. To see a list of available options press `?` followed by the `Enter` key. To quit, press `q` then `Enter`.

### Testing

If you don't have the [Galactic Aztec Heavy Raspberry Pi Add-on: ADC], or similar board available, then you can start the server in testing mode (whereas it generates sinusoidal test data):
```
server/bin/server --test
```


## Client

[![Client Gauges Screenshot](artwork/thumb_client_gauges.png?raw=true)](artwork/client_gauges.png?raw=true)
[![Client Graphs Screenshot](artwork/thumb_client_graphs.png?raw=true)](artwork/client_graphs.png?raw=true)

### OS X

Simply download and install the OS X package (named `DataAcquisitionClient.dmg`) from the [latest release] page.

### Windows

Locate and download the latest `DataAcquisitionClient.zip` file from the [releases page]. Extract and run `DataAcquisitionClient.exe`.

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


[Galactic Aztec Data Acquisition]: https://github.com/twyatt/galactic-aztec-data-acquisition
[Galactic Aztec Heavy Raspberry Pi Add-on: ADC]: https://github.com/twyatt/galactic-aztec-heavy-rpi-addon-adc
[SDSU Rocket Project]: http://rocket.sdsu.edu/
[Galactic Aztec Heavy]: http://rocket.sdsu.edu/rockets#galactic-aztec-heavy
[latest release]: https://github.com/twyatt/galactic-aztec-heavy-data-acquisition/releases/latest
[releases page]: https://github.com/twyatt/galactic-aztec-heavy-data-acquisition/releases
[Java SE Development Kit 8]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[latest Source code (zip)]: https://github.com/twyatt/galactic-aztec-heavy-data-acquisition/zipball/master