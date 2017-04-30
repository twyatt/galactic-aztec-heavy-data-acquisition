import binascii
import socket
import struct
import sys

UDP_IP = "127.0.0.1"
UDP_PORT = 6666

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP

# Datagram Packet Format:
# |--------|-------|-------------------------------------------|----------------------|
# | Type   | Name  | Description                               | Client Expected Unit |
# |--------|-------|-------------------------------------------|----------------------|
# | int    | time  | Gyroscope Timestamp                       | milliseconds         |
# | float  | gx    | Gyroscope X                               | deg/s                |
# | float  | gy    | Gyroscope Y                               | deg/s                |
# | float  | gz    | Gyroscope Z                               | deg/s                |
# | int    | time  | Accelerometer Timestamp                   | milliseconds         |
# | float  | ax    | Accelerometer X                           | g                    |
# | float  | ay    | Accelerometer Y                           | g                    |
# | float  | az    | Accelerometer Z                           | g                    |
# | int    | time  | Inclinometer Timestamp                    | milliseconds         |
# | float  | ix    | Inclinometer X                            | g                    |
# | float  | iy    | Inclinometer Y                            | g                    |
# | float  | iz    | Inclinometer Z                            | g                    |
# | byte   | stat  | GPS Fix Status (1 = none, 2 = 2d, 3 = 3d) |                      |
# | int    | sat   | GPS Number of Satellites                  |                      |
# | int    | time  | GPS Timestamp                             | milliseconds         |
# | double | lat   | GPS Latitude                              |                      |
# | double | lng   | GPS Longitude                             |                      |
# | double | alt   | GPS Altitude                              | meters MSL           |
# |--------|-------|-------------------------------------------|----------------------|
#
# Recommended unit for timestamp is milliseconds (which overflows at ~24 days)

values = (1000, 1.0, 2.0, 3.0, 1000, 4.0, 5.0, 6.0, 1000, 7.0, 8.0, 9.0, 3, 9, 1000, 32.777928, -117.070138, 2000.0)

# Python struct format
# https://docs.python.org/2/library/struct.html
packer = struct.Struct('!i f f f i f f f i f f f b i i d d d')

packed_data = packer.pack(*values)

try:
	print >>sys.stderr, 'sending "%s"' % binascii.hexlify(packed_data), values
	sock.sendto(packed_data, (UDP_IP, UDP_PORT))

finally:
    print >>sys.stderr, 'closing socket'
    sock.close()
