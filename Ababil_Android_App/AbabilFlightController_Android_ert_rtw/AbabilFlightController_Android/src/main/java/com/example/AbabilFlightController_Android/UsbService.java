package com.example.AbabilFlightController_Android;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Copyright 2019 The MathWorks, Inc.
 */

public class UsbService extends Service {

    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    public static final String ACTION_USB_CONNECTION_UNSUCCESSFUL = "com.mathworks.usbservide.ACTION_USB_CONNECTION_UNSUCCESSFUL";
    /*public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;*/
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int BAUD_RATE = 9600; // BaudRate. Change this value if you need
    public static boolean SERVICE_CONNECTED = false;

    private final IBinder binder = new UsbBinder();

    private Context context;
    /*private Handler mHandler;*/
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    private BlockingQueue<byte[]> queue;

    private boolean serialPortConnected;
    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            switch (arg1.getAction()) {
                case ACTION_USB_PERMISSION:
                    boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted) // User accepted our USB connection. Try to open the device as a serial port
                    {
                        Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                        arg0.sendBroadcast(intent);
                        try {
                            connection = usbManager.openDevice(device);
                            new ConnectionThread().start();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            intent = new Intent(ACTION_USB_CONNECTION_UNSUCCESSFUL);
                            arg0.sendBroadcast(intent);
                        }
                    } else // User not accepted our USB connection. Send an Intent to the Main Activity
                    {
                        Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                        arg0.sendBroadcast(intent);
                    }
                    break;
                case ACTION_USB_ATTACHED:
                    if (!serialPortConnected)
                        findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
                    break;
                case ACTION_USB_DETACHED:
                    // Usb device was disconnected. send an intent to the Main Activity
                    Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                    arg0.sendBroadcast(intent);
                    if (serialPortConnected) {
                        serialPort.close();
                    }
                    serialPortConnected = false;
                    clearSerialQueue();
                    break;
            }
        }
    };

    private final byte[] brokenPiece = new byte[1024];
    private int brokenLength;
    /*
     *  Data received from serial port will be received here. Just populate onReceivedData with your code
     *  In this particular example. byte stream is converted to String and send to UI thread to
     *  be treated there.
     */
    private final UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            try {
                if (arg0.length > 0)
                    queue.put(arg0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
    /*
     * State changes in the CTS line will be received here
     */
    /*private UsbSerialInterface.UsbCTSCallback ctsCallback = new UsbSerialInterface.UsbCTSCallback() {
        @Override
        public void onCTSChanged(boolean state) {
            if (mHandler != null)
                mHandler.obtainMessage(CTS_CHANGE).sendToTarget();
        }
    };*/
    /*
     * State changes in the DSR line will be received here
     */
    /*private UsbSerialInterface.UsbDSRCallback dsrCallback = new UsbSerialInterface.UsbDSRCallback() {
        @Override
        public void onDSRChanged(boolean state) {
            if (mHandler != null)
                mHandler.obtainMessage(DSR_CHANGE).sendToTarget();
        }
    };*/

    /*
     * onCreate will be executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
     */
    @Override
    public void onCreate() {
        this.context = this;
        serialPortConnected = false;
        UsbService.SERVICE_CONNECTED = true;
        queue = new ArrayBlockingQueue<>(BAUD_RATE);
        setFilter();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();
    }

    /* MUST READ about services
     * http://developer.android.com/guide/components/services.html
     * http://developer.android.com/guide/components/bound-services.html
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
        UsbService.SERVICE_CONNECTED = false;
        clearSerialQueue();
    }

    /*
     * This function will be called from MainActivity to write data through Serial Port
     */
    public void write(byte[] data) {
        if (serialPort != null)
            serialPort.write(data);
    }

    /*public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }*/

    /*
     * This function will be called from MainActivity to change baud rate
     */
    public void setBaudRate(int baudRate) {
        if (serialPort != null)
            serialPort.setBaudRate(baudRate);
    }

    public boolean checkSerialQueueStatus(int dataSizeInBytes) {
        int count = 0;
        for (byte[] aQueue : queue) {
            count += aQueue.length;
        }
        return count >= dataSizeInBytes;
    }

    public byte[] readSerialQueueBlocking(int dataSizeInBytes) {
        byte[] ret = new byte[dataSizeInBytes];
        int index = 0;
        if (brokenLength != 0) {
            if (dataSizeInBytes >= brokenLength) {
                System.arraycopy(brokenPiece, 0, ret, 0, brokenLength);
                dataSizeInBytes -= brokenLength;
                index += brokenLength;
                brokenLength = 0;
            } else {
                System.arraycopy(brokenPiece, 0, ret, 0, dataSizeInBytes);
                brokenLength -= dataSizeInBytes;
                System.arraycopy(brokenPiece, dataSizeInBytes, brokenPiece, 0, brokenLength);
                return ret;
            }
        }
        while (dataSizeInBytes > 0) {
            byte[] first = queue.remove();
            if (first.length <= dataSizeInBytes) {
                System.arraycopy(first, 0, ret, index, first.length);
                dataSizeInBytes -= first.length;
                index += first.length;
            } else {
                brokenLength = first.length - dataSizeInBytes;
                System.arraycopy(first, 0, ret, index, dataSizeInBytes);
                System.arraycopy(first, dataSizeInBytes, brokenPiece, 0, brokenLength);
                dataSizeInBytes = 0;
                index = 0;
            }
        }
        return ret;
    }

    public void clearSerialQueue() {
        queue.clear();
        brokenLength=0;
    }

    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();

//                if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003) && deviceVID != 0x5c6 && devicePID != 0x904c) {
                if (!isUsbRootHub(device)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
            if (!keep) {
                // There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);
            }
        } else {
            // There is no USB devices connected. Send an intent to MainActivity
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

    private boolean isUsbRootHub(UsbDevice usbDevice) {
        return usbDevice.getVendorId() == 0x1d6b && usbDevice.getProductId() < 4;
    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(usbReceiver, filter);
    }

    /*
     * Request user permission. The response will be received in the BroadcastReceiver
     */
    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    class UsbBinder extends Binder {
        UsbService getService() {
            return UsbService.this;
        }
    }

    /*
     * A simple thread to open a serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null) {
                if (serialPort.open()) {
                    serialPortConnected = true;
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    /*
                      Current flow control Options:
                      UsbSerialInterface.FLOW_CONTROL_OFF
                      UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                      UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                     */
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mCallback);
                    /*serialPort.getCTS(ctsCallback);
                    serialPort.getDSR(dsrCallback);
                    serialPort.debug(true);*/

                    //
                    // Some Arduinos would need some sleep because firmware wait some time to know whether a new sketch is going 
                    // to be uploaded or not
                    //Thread.sleep(2000); // sleep some. YMMV with different chips.

                    // Everything went as expected. Send an intent to MainActivity
                    Intent intent = new Intent(ACTION_USB_READY);
                    context.sendBroadcast(intent);
                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if (serialPort instanceof CDCSerialDevice) {
                        Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        context.sendBroadcast(intent);
                    } else {
                        Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        context.sendBroadcast(intent);
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(intent);
            }
        }
    }
}
