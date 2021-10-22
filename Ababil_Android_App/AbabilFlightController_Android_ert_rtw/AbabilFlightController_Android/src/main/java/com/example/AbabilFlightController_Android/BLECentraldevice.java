package com.example.AbabilFlightController_Android;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.UUID;

/**
 * BLECentraldevice.java
 * <p/>
 * Each client block information is stored in this class
 * <p/>
 * Copyright 2017 The MathWorks, Inc.
 */

public class BLECentraldevice {
    static final String TAG = "BLECentraldevice";
    UUID CharacteristicUUID;
    int blockID;
    BLEClientConnection bLEClientConnection;
    BLECentraldevice(UUID uuid, BLEClientConnection connection) {
        CharacteristicUUID = uuid;
        bLEClientConnection = connection;
    }
    
    byte[] getValue() {
        BluetoothGattCharacteristic chars=bLEClientConnection.getCharacteristic(CharacteristicUUID);
        Log.d(TAG,"char in central" + chars);
        if (chars != null)
            return chars.getValue();
        else
            return null;
    }
    
    void setValue(byte[] chars) {
        bLEClientConnection.setCharacteristic(CharacteristicUUID, chars);
    }
}
