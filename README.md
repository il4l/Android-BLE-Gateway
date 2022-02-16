# InLoc4Log Android BLE Gateway

This repository contains an android application that acts as a gateway for InLoc4Log BLE beacons (commonly referred to as tag or lite). The gateway connects to one or more beacons and sends all ranging data to a MQTT broker.
This app is based on [Nordic nRF Blinky](https://github.com/NordicSemiconductor/Android-nRF-Blinky).

## Note

In order to scan for Bluetooth LE device the Location permission must be granted and, on some phones,
the Location must be enabled. This app will not use the location information in any way.

## BLE Service

Service UUID: `8FA50001-BCC2-4BE4-B49E-4F5C34546F6C`

The services provides a few different GATT characteristics. The two main ones are Ranging and Version.

All values are unsigned 16-bit integers.
Values inside `[` and `]` denote that they're repeating (arrays) based on a prepended size variable (LiteCount).

| Name      | Description                           |
|-----------|---------------------------------------|
| ID        | Node-ID                               |
| Range     | Distance to the tag                   |
| FWVersion | MSB = main version, LSB = sub version |

### Ranging (advertising characteristic)

Send by the beacon to anounce current ranging information.

UUID: `8FA50006-BCC2-4BE4-B49E-4F5C34546F6C`

Values: `LiteCount, [ID, Range]`

### Version (readable characteristic)

Query tag version.

UUID: `8FA50003-BCC2-4BE4-B49E-4F5C34546F6C`

Values: `LiteCount, [ID, FWVersion]`