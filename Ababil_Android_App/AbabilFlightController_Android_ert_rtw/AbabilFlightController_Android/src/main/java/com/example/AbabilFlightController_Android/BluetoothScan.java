package com.example.AbabilFlightController_Android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;



public class BluetoothScan {
    BluetoothAdapter mBluetoothAdapter;
    Timer mTimer = new Timer();
    private HashMap<String, String> mDeviceList = new HashMap<String, String>();
    private boolean mScanning = false;
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (device != null) {
                        if (device.getName() != null)
                            mDeviceList.put(device.getName(), device.getAddress());
                    }
                }
            };
    TimerTask mTimerTask = new TimerTask() {

        @Override
        public void run() {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    };

    BluetoothScan(BluetoothAdapter adapter) {
        mBluetoothAdapter = adapter;
    }

    void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.

            mTimer.schedule(mTimerTask, 50000);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    synchronized String getDeviceAddress(String name) {
        Iterator it = mDeviceList.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry) it.next();
            String g = (String) pair.getValue();
            String h = (String) pair.getKey();
            if (h.equals(name))
                return g;
        }
        return null;
    }

}
