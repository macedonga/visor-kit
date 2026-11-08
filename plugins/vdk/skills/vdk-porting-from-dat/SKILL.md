---
name: vdk-porting-from-dat
description: Port an app from Meta's Wearables Device Access Toolkit (DAT) to the VDK
---

# Porting from DAT

The VDK covers DAT's capabilities through Visor, with no Meta AI app, registration or developer centre. Most DAT
types have a one-to-one VDK name: sessions become `Visor.connect`, `MockDeviceKit` becomes `FakeVisor`,
`display.sendContent { flexBox { } }` becomes `visor.display.layout.show(Layout { })`.

Read the mapping tables with `read_vdk_doc` and the page `porting-from-dat` before changing code.
