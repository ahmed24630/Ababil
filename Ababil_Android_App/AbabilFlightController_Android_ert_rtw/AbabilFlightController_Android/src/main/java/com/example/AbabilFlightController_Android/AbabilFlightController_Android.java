package com.example.AbabilFlightController_Android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;
import java.util.ArrayList;
import android.content.res.Configuration;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;
import java.util.Hashtable;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import java.util.Arrays;
import java.util.UUID;
import java.util.Iterator;
import java.util.HashMap;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import java.util.Set;

public class AbabilFlightController_Android extends AppCompatActivity implements SensorEventListener, OnFragmentInteractionListener {
    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;
    private InfoFragment infoFragment = null;
     private Hashtable<Integer,TextView> textViews = new Hashtable<Integer,TextView>();
     private float[] mGyroscopeData = { 0.0f, 0.0f, 0.0f };
     private float[] mAccelerometerData = { 0.0f, 0.0f, 0.0f };
     private float[] mMagnetometerData = { 0.0f, 0.0f, 0.0f };
     private SensorManager mSensorManager;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 300;
    private boolean isFineLocationPermissionGranted = false;
    private boolean isFineLocationPermissionRequested = false;
      private BluetoothAdapter mBluetoothAdapter; 
 HashMap<String, BLEClientConnection> mConnectionList = new HashMap<String, BLEClientConnection>(); 
      HashMap<Integer, BLECentraldevice> mCentralList = new HashMap<Integer, BLECentraldevice>();
      HashMap<Integer, BLEPeripheralDevice> mPeripheralList = new HashMap<Integer, BLEPeripheralDevice>();
      BLEServerConnection mBLEServerConnection = null;
      BluetoothScan mBluetoothScan = null;

     private void registerSensorManager() {
        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_FASTEST);
     }

    void setupClient(int blockId, String serviceUUID,String charUUID, String address, String name) {
        BLEClientConnection connection = getBLEClientConnection(address,name);
        UUID newcharUUID = UUID.fromString(charUUID);
        BLECentraldevice cd = new BLECentraldevice(newcharUUID, connection);
        mCentralList.put(blockId,cd);
    }
    BLEClientConnection getBLEClientConnection(String address, String name) {
        if( mConnectionList.containsKey(address)){
            return mConnectionList.get(address);
         }
        else {
            BLEClientConnection conn =  new BLEClientConnection(address, mBluetoothAdapter, name, this, mBluetoothScan);
            conn.createConnection();
            mConnectionList.put(address,conn);
            return conn;
}
}

    byte[] stepClientReceive(int blockId) {
        BLECentraldevice bc = mCentralList.get(blockId);
        byte[] data = null;
        if (bc != null && bc.getValue() != null){
        byte[] value = bc.getValue();
        data = Arrays.copyOf(bc.getValue(), bc.getValue().length);
}
return data;
    }
    void stepClientSend(int blockId,byte[] chars) {
         BLECentraldevice bc = mCentralList.get(blockId);
         if (bc != null)
         bc.setValue(chars);}
    void setupServer(int BlockId, String serviceUUID, String charUUID) {
         Log.d("BLEServer","blockid" + BlockId + "serviceuuid " + serviceUUID + " charuuid " + charUUID + " mBLEServerConnection " + mBLEServerConnection);         if (mBLEServerConnection == null){
             mBLEServerConnection = BLEServerConnection.getInstance(this);
 mBLEServerConnection.startserver(this);
}
             mPeripheralList.put(BlockId, new BLEPeripheralDevice(UUID.fromString(serviceUUID),UUID.fromString(charUUID),mBLEServerConnection));
}
    void stepServerSend(int blockID, byte[] chars) {
         BLEPeripheralDevice bp = mPeripheralList.get(blockID);
         if(bp != null)
         bp.sendCharacteristic(chars);}
    byte[] stepServerReceive(int blockID) {
         BLEPeripheralDevice bp = mPeripheralList.get(blockID);
         byte[] chars = null;
         if (bp != null) {
             byte[] charValue = bp.receiveCharacteristic();
             if (charValue != null)
                  chars = Arrays.copyOf(charValue, charValue.length);
}
         return chars;
}
    void closeBLEConections(){
         Iterator it =  mConnectionList.entrySet().iterator();
         while(it.hasNext()) {
             HashMap.Entry pair = (HashMap.Entry)it.next();             BLEClientConnection g = (BLEClientConnection)pair.getValue();
             g.close();
}
}

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_CONNECTION_UNSUCCESSFUL :
                    Toast.makeText(context, "Exception: USB connection unsuccessful", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    private boolean checkIfAllPermissionsGranted()
    {
        return true && isFineLocationPermissionGranted;
    }
    private void requestPermission() {
        String permissionRationale = "";
        // Here, thisClass is the current activity
        //request for fine location
        if (ContextCompat.checkSelfPermission(thisClass,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted. Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(thisClass,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                permissionRationale += "Access fine location, ";
            } else {
                // No explanation needed; request the permission
                if (!isFineLocationPermissionRequested) {
                    isFineLocationPermissionRequested = true;
                    ActivityCompat.requestPermissions(thisClass,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                    return;
                }
            }
        } else {
            // Permission has already been granted
            isFineLocationPermissionGranted = true;
        mBluetoothScan.scanLeDevice(true);
        }
        if (!permissionRationale.isEmpty())
            if (infoFragment != null) {
                infoFragment.updateModelInfo(permissionRationale + "permission not granted. Model cannot start.");
            }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //Uncomment the following line to specify a custom App Title
        //toolbar.setTitle("My custom Title");
        setSupportActionBar(toolbar);

        // Create a FragmentPagerAdapter that returns individual fragments
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(SectionsPagerAdapter.getNumTabs()-1);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        // Initiate the SensorManager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
       final BluetoothManager bluetoothManager = 
       (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothScan = new BluetoothScan(mBluetoothAdapter);
        thisClass = this;
     }

    private AbabilFlightController_Android thisClass;
    private final Thread BgThread = new Thread() {
    @Override
    public void run() {
            String argv[] = new String[] {"MainActivity","AbabilFlightController_Android"};
            naMain(argv, thisClass);
        }
    };

    public void flashMessage(final String inMessage) {
        runOnUiThread(new Runnable() {
              public void run() {
                    Toast.makeText(getBaseContext(), inMessage, Toast.LENGTH_SHORT).show();
              }
        });
    }

    protected void onDestroy() {
         if (BgThread.isAlive())
             naOnAppStateChange(6);
         closeBLEConections();
         if (mBLEServerConnection !=null)
             mBLEServerConnection.stopServer();
         super.onDestroy();
         System.exit(0); //to kill all our threads.
    }

	@Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof InfoFragment) {
            this.infoFragment = (InfoFragment) fragment;
            infoFragment.setFragmentInteractionListener(this);
            requestPermission();
        }
        if (fragment instanceof AppFragment) {
            ((AppFragment)fragment).setFragmentInteractionListener(this);
        }
    }

	@Override
    public void onFragmentCreate(String name) {

    }

    @Override
    public void onFragmentStart(String name) {
        switch (name) {
            case "Info":
               break;
            case "App":
                registerDataDisplays();
                break;
            default:
                break;
    }
    }

    @Override
    public void onFragmentResume(String name) {
        switch (name) {
            case "App":
                break;
            case "Info":
                if (checkIfAllPermissionsGranted()){
                    if (!BgThread.isAlive()) {
                        BgThread.start();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onFragmentPause(String name) {
    }
    @Override
    protected void onResume() {
         super.onResume();
         if (BgThread.isAlive())
             naOnAppStateChange(3);
         registerSensorManager();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    protected void onPause() {
        if (BgThread.isAlive())
            naOnAppStateChange(4);
         mSensorManager.unregisterListener(this);
        if (usbService!=null) {
            usbService.clearSerialQueue();
        }
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do related task you need to do.
                    isFineLocationPermissionGranted = true;
        mBluetoothScan.scanLeDevice(true);
                } else {
                    // permission denied, boo!
                    flashMessage("Access location Permission not granted");
                }
                isFineLocationPermissionRequested = false;
                break;

            // other case lines to check for other
            // permissions this app might request.
        }
        if (!checkIfAllPermissionsGranted()) {
            requestPermission();
        }
    }

    public void registerDataDisplays() {
    // bind text views for data display block;
    for (int i = 1; i <= 1; i++) {
            TextView textView = (TextView) findViewById(
            getResources().getIdentifier("DataDisplay" + i, "id", getPackageName()));
            textViews.put(i, textView);
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float [] values = event.values;
        //Comment out if you want to log the data in logcat
        //String logMessage = String.format("%d: 0'%g'", event.sensor.getType(), values[0]);
        //Log.d("Sensor Data IN:", logMessage);
        switch(event.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE:
                mGyroscopeData[0] = values[0];
                mGyroscopeData[1] = values[1];
                mGyroscopeData[2] = values[2];
                break;
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerData[0] = values[0];
                mAccelerometerData[1] = values[1];
                mAccelerometerData[2] = values[2];
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetometerData[0] = values[0];
                mMagnetometerData[1] = values[1];
                mMagnetometerData[2] = values[2];
                break;
        }
    }

    // Get SensorEvent Data throws exception if the data is null
    public float[] getGyroscopeData() {
        return mGyroscopeData;
    }

    public float[] getAccelerometerData() {
        return mAccelerometerData;
    }

    public float[] getMagnetometerData() {
        return mMagnetometerData;
    }

    public void displayText(int id, byte[] data, byte[] format) {
        String formatString = new String(format);
        String toDisplay = String.format(formatString, data[0]);
        if (data.length > 1) {
            for (int i = 1; i < data.length; i++)
                toDisplay += "\n" + String.format(formatString, data[i]);
        }
        updateTextViewById(id, toDisplay);
    }

    public void displayText(int id, short[] data, byte[] format) {
        String formatString = new String(format);
        String toDisplay = String.format(formatString, data[0]);
        if (data.length > 1) {
            for (int i = 1; i < data.length; i++)
                toDisplay += "\n" + String.format(formatString, data[i]);
        }
        updateTextViewById(id, toDisplay);
    }

    public void displayText(int id, int[] data, byte[] format) {
        String formatString = new String(format);
        String toDisplay = String.format(formatString, data[0]);
        if (data.length > 1) {
            for (int i = 1; i < data.length; i++)
                toDisplay += "\n" + String.format(formatString, data[i]);
        }
        updateTextViewById(id, toDisplay);
    }

    public void displayText(int id, long[] data, byte[] format) {
        String formatString = new String(format);
        String toDisplay = String.format(formatString, data[0]);
        if (data.length > 1) {
            for (int i = 1; i < data.length; i++)
                toDisplay += "\n" + String.format(formatString, data[i]);
        }
        updateTextViewById(id, toDisplay);
    }

    public void displayText(int id, float[] data, byte[] format) {
        String formatString = new String(format);
        String toDisplay = String.format(formatString, data[0]);
        if (data.length > 1) {
            for (int i = 1; i < data.length; i++)
                toDisplay += "\n" + String.format(formatString, data[i]);
        }
        updateTextViewById(id, toDisplay);
    }

    public void displayText(int id, double[] data, byte[] format) {
        String formatString = new String(format);
        String toDisplay = String.format(formatString, data[0]);
        if (data.length > 1) {
            for (int i = 1; i < data.length; i++)
                toDisplay += "\n" + String.format(formatString, data[i]);
        }
        updateTextViewById(id, toDisplay);
    }

    private void updateTextViewById(final int id, final String finalStringToDisplay) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    textViews.get(id).setText(finalStringToDisplay);
                } catch (Exception ex) {
                    Log.e("AbabilFlightController_Android.updateTextViewById", ex.getLocalizedMessage());
                }
            }
        });
    }
    public byte[] readSerialBytes(int length, byte[] sts) {
        if (usbService!= null && usbService.checkSerialQueueStatus(length)) {
            sts[0] = 1;
            return usbService.readSerialQueueBlocking(length);
        }

        sts[0] = 0;
        return new byte[length];
    }

    public void writeSerialBytes(byte[] data) {
        if (usbService!=null) {
            usbService.write(data);
        }
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private native int naMain(String[] argv, AbabilFlightController_Android pThis);
    private native void naOnAppStateChange(int state);
    static {
        System.loadLibrary("AbabilFlightController_Android");
    }

}
