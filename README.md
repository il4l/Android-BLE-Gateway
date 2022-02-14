# InLoc4Log Android BLE Gateway

This repository contains an android application that acts as a gateway for InLoc4Log BLE beacons (commonly referred to as tags). The gateway connects to one or more beacons and sends all data to a MQTT broker.
This app is based on [Nordic nRF Blinky](https://github.com/NordicSemiconductor/Android-nRF-Blinky).

## Note

In order to scan for Bluetooth LE device the Location permission must be granted and, on some phones, 
the Location must be enabled. This app will not use the location information in any way.
