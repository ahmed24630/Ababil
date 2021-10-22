package com.example.AbabilFlightController_Android;

import android.content.Context;

import java.util.UUID;

/**
 * BLEPeripheraldevice.java
 * <p/>
 * Each Peripheral block information is stored in this class
 * <p/>
 * Copyright 2017 The MathWorks, Inc.
 */

public class BLEPeripheralDevice {
    int blockId;
    UUID serviceUUID;
    UUID charUUID;
    BLEServerConnection bleServerConnection;
    
    BLEPeripheralDevice(UUID serviceUUID, UUID charUUID, BLEServerConnection bleServerConnection) {
        this.serviceUUID =serviceUUID;
        this.charUUID=charUUID;
        this.bleServerConnection=bleServerConnection;
    }
    
    void sendCharacteristic(byte[] value){
        bleServerConnection.sendCharacteristic(value, charUUID,null);
    }
    byte[] receiveCharacteristic() {
        return  bleServerConnection.receiveCharacteristic(charUUID);
    }
}
