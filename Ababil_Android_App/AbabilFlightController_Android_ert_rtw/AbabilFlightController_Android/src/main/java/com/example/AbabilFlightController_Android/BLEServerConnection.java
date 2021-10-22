package com.example.AbabilFlightController_Android;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static android.content.Context.BLUETOOTH_SERVICE;



/**
 * BLEServerConnection.java
 * <p/>
 * Server connection is managed by this class
 * <p/>
 * Copyright 2017 The MathWorks, Inc.
 */

public class BLEServerConnection {
    static final String TAG = "BLEServerConnection";
    static BLEServerConnection mBLEServerConnection;
    BluetoothGatt mBluetoothGatt;
    BluetoothGattServer mGattServer;
    BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    BluetoothLeAdvertiser mbluetoothAdvertiser;
    HashMap<UUID, BluetoothGattCharacteristic> mCharMap = new HashMap<UUID, BluetoothGattCharacteristic>();
    List<BluetoothDevice> mConnectedDevices = new ArrayList<BluetoothDevice>();
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "onStartSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.d(TAG, "onStartFailure");
        }
    };
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);


            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectedDevices.add(device);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectedDevices.remove(device);
            }


        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device,
                                                int requestId,
                                                int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            byte[] value = mCharMap.get(characteristic.getUuid()).getValue();
            mGattServer.sendResponse(
                    device, requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    value);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            if (responseNeeded) {
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        value);
            }
            sendCharacteristic(value, characteristic.getUuid(), device);
        }

    };

    private BLEServerConnection(Context context) { 
    }
       
    void startserver(Context context) {
        BluetoothManager manager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothLeAdvertiser = manager.getAdapter().getBluetoothLeAdvertiser();
        mGattServer = manager.openGattServer(context, mGattServerCallback);
        addServices(context);
        startAdvertising();
    }
    
    public static synchronized BLEServerConnection getInstance(Context context) {
        if (mBLEServerConnection == null)
            mBLEServerConnection = new BLEServerConnection(context);

        return mBLEServerConnection;
    }

    private void addServices(Context context) {
        int id = context.getResources().getIdentifier("server", "array", context.getPackageName());
        String[] list = context.getResources().getStringArray(id);
        HashMap<UUID, BluetoothGattService> serviceMap = new HashMap<UUID, BluetoothGattService>();
        for (int i = 0; i < list.length; i++) {
            Log.d(TAG, "list item " + list[i]);
            String[] uuid = list[i].split("_");
            Log.d(TAG, "list item " + uuid[0] + " " + uuid[1]);
            UUID seruuid = UUID.fromString(uuid[0]);
            BluetoothGattService service = serviceMap.get(seruuid);

            UUID charUUID = UUID.fromString(uuid[1]);
            BluetoothGattCharacteristic Characteristic =
                    new BluetoothGattCharacteristic(charUUID,
                            //Read-only characteristic, supports notifications
                            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_WRITE,
                            BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
            mCharMap.put(charUUID, Characteristic);
            if (service == null) {
                Log.d(TAG,"service null " + seruuid);
                service = new BluetoothGattService(seruuid,
                        BluetoothGattService.SERVICE_TYPE_PRIMARY);
                serviceMap.put(seruuid, service);
            }
            Log.d(TAG,"addcharacteristic " + seruuid + " gh " + charUUID);
            service.addCharacteristic(Characteristic);
        }
        Iterator it = serviceMap.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry) it.next();
            BluetoothGattService g = (BluetoothGattService) pair.getValue();
             Log.d(TAG,"service added " + g.getUuid());
            mGattServer.addService(g);
        }
    }

    private void startAdvertising() {
        Log.d(TAG, "start advertising");


        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }
    
    void sendCharacteristic(byte[] value, UUID charUUID, BluetoothDevice device) {
        BluetoothGattCharacteristic readCharacteristic = mCharMap.get(charUUID);
        if (readCharacteristic != null) {
            readCharacteristic.setValue(value);
            if (device != null)
                mGattServer.notifyCharacteristicChanged(device, readCharacteristic, false);
            else {
                 for (BluetoothDevice device1 : mConnectedDevices) {
                     mGattServer.notifyCharacteristicChanged(device1, readCharacteristic, false);
                 }
            }
        }
    }

    byte[] receiveCharacteristic(UUID charUUID) {
        BluetoothGattCharacteristic chars = mCharMap.get(charUUID);
        return chars.getValue();
    }

    void stopServer() {
        if (mBluetoothLeAdvertiser == null) return;
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
         for (BluetoothDevice device : mConnectedDevices) {
             mGattServer.cancelConnection(device);
         }
        mGattServer.close();
    }
}
