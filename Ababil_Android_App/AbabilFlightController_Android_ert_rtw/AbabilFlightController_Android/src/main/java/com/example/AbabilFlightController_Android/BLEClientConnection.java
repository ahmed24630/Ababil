package com.example.AbabilFlightController_Android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;



/**
 * BLEClientConnection.java
 * <p/>
 * Client connection is managed by this class
 * <p/>
 * Copyright 2017 The MathWorks, Inc.
 */


class BLEClientConnection {
    public static final String CLIENT_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    static final String TAG = "BLEClientConnection";
    public String pheripheralAddress = "";
    public BluetoothDevice mBluetoothDevice;
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothGatt mBluetoothGatt;
    public BluetoothGattService mBluetoothGattService;
    Boolean mConnected = true;
    public HashMap<UUID, BluetoothGattCharacteristic> mcharmap = new HashMap<UUID, BluetoothGattCharacteristic>();
    BluetoothScan scan;
    int trials = 0;
    String name = "";
    Timer mTimer = null;
    Context context;
    UUID[] uuidlist;
    
    public BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {               
                if (status == 0) {
                mBluetoothGatt.discoverServices();
                mConnected = true;
                }
            } else if (newState ==BluetoothProfile.STATE_DISCONNECTED ){
                mConnected = false;
            }
            if (status !=0 || (newState ==BluetoothProfile.STATE_DISCONNECTED)) {
                mConnected = false;
                mcharmap = null;
               if (mTimer == null) {
                    mTimer = new Timer();
                   MyTimertask mTimertask = new MyTimertask();
                    mTimer.schedule(mTimertask,1,30000);
               }
            }
        }
        
        class MyTimertask extends TimerTask {
            @Override
            public void run() {
               if (!mConnected)
                   createConnection();
               else {

                   mTimer.cancel();
                   mTimer = null;
               }
           }
       }
    
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                getdiscoveredServices(gatt);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (mcharmap != null)
            mcharmap.put(characteristic.getUuid(), characteristic);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (mcharmap != null)
            mcharmap.put(characteristic.getUuid(), characteristic);

        }
    };

    public BLEClientConnection(String pheripheralAddress, BluetoothAdapter bluetoothAdapter, String name, Context context, BluetoothScan scan) {
        this.pheripheralAddress = pheripheralAddress;
        this.context = context;
        this.name = name;
        uuidlist = getUUIDList();
        this.scan = scan;
        mBluetoothAdapter = bluetoothAdapter;
    }

    UUID[] getUUIDList() {
        String address = "a" + pheripheralAddress.replace(":", "") + "a";
        int id = context.getResources().getIdentifier(address, "array", context.getPackageName());
        int id1 = context.getResources().getIdentifier("error_information", "array", context.getPackageName());
        String[] list = context.getResources().getStringArray(id);
        UUID[] uuidlist = new UUID[list.length];
        for (int i = 0; i < list.length; i++) {
            uuidlist[i] = UUID.fromString(list[i]);
            Log.d(TAG, " uuid " + uuidlist[i].toString());
        }
        return uuidlist;
    }

    int createConnection() {
        String newaddress = scan.getDeviceAddress(name);
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(newaddress == null ? pheripheralAddress : newaddress);
        Handler mHandler = new Handler(context.getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt = mBluetoothDevice.connectGatt(context, false, mBluetoothGattCallback);
            }
        });
        Log.d(TAG, "createconnection" + mBluetoothGatt);
        return 1;
    }

    void getdiscoveredServices(BluetoothGatt gatt) {
        List<BluetoothGattService> ser = gatt.getServices();
        List<UUID> stringList = Arrays.asList(uuidlist);
        mcharmap = new HashMap<UUID, BluetoothGattCharacteristic>();
        for (BluetoothGattService gattService : ser) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                mcharmap.put(gattCharacteristic.getUuid(), gattCharacteristic);
                if (stringList.contains(gattCharacteristic.getUuid())) {
                    mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);

                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(
                            UUID.fromString(CLIENT_CONFIG_UUID));

                    if (descriptor != null) {

                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBluetoothGatt.writeDescriptor(descriptor);
                        Log.d(TAG, "descriptor");
                    }
                    mBluetoothGatt.readCharacteristic(gattCharacteristic);
                }
            }
        }
    }


    BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
        BluetoothGattCharacteristic gattCharacteristic = null;
        if (mcharmap != null) {
            gattCharacteristic =  mcharmap.get(uuid);
        if(gattCharacteristic != null) {
        final int charaProp = gattCharacteristic.getProperties();
        if (!((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)) {
            mBluetoothGatt.readCharacteristic(gattCharacteristic);
        }
    }
        }
    return gattCharacteristic;
    }

    void setCharacteristic(UUID uuid, byte[] chars) {
        if (mcharmap != null) {
        BluetoothGattCharacteristic bc = mcharmap.get(uuid);
        if (bc != null) {
            bc.setValue(chars);
            mBluetoothGatt.writeCharacteristic(bc);
        }
        }
    }

    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
    }
    }
}

