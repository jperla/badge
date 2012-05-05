package com.jperla.badge;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ViewSwitcher;

import java.io.IOException;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothServerSocket;
import android.content.Intent;
import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BadgeActivity extends Activity implements SensorEventListener {
    ViewSwitcher switcher;
    CameraSurfaceView cam_surface;
    SensorManager sm;
    Sensor acc_sensor;
    Sensor mag_sensor;
    float[] acceleration_vals = null;
    float[] magnetic_vals = null;

    String LOG_TAG = "------- Badge -------";
    
    BluetoothAdapter bt_adapter;

    static final int BT_ENABLE_ACTIVITY = 1;
    static final UUID BT_UUID = UUID.fromString("ff15e609-34b1-4b49-9d40-a650566c8960");
    static final String joe_mac = "9C:02:98:70:23:67";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        switcher = (ViewSwitcher) findViewById(R.id.modeSwitcher);

        // Create an area to preview face detection results, and fetch camera
        cam_surface = (CameraSurfaceView) findViewById(R.id.surface_view);

        // Fetch the sensor manager.
        Context c = switcher.getContext();
        sm = (SensorManager) c.getSystemService(SENSOR_SERVICE);

        // Register a listener for the accelerometer.
        acc_sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Register a listener for the magnetic field sensor.
        mag_sensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sm.registerListener(this, mag_sensor, SensorManager.SENSOR_DELAY_NORMAL);



        // Fetch the Bluetooth adapter and make sure it is enabled.
        bt_adapter = BluetoothAdapter.getDefaultAdapter();
        if(bt_adapter == null) {
            Log.d("=badge=", "ERROR: No Bluetooth adapter found");
        }
        else {
            Log.d("=badge=", "Successfully found Bluetooth adapter");
        }

        Log.d("=badge=", BT_UUID.toString());

    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm.registerListener(this, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, mag_sensor, SensorManager.SENSOR_DELAY_NORMAL);

        if (bt_adapter.isEnabled()) {
            Log.d("=badge=", "Bluetooth already enabled");
            tryConnection();
        }
        else {
            Log.d("=badge=", "Requesting to enable Bluetooth");
            Intent enable_bt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable_bt, BT_ENABLE_ACTIVITY);
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        // Get the data from the sensor.
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                acceleration_vals = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetic_vals = event.values.clone();
                break;
        }

        // If we have all the necessary data, update the orientation.
        if (acceleration_vals != null && magnetic_vals != null) {
            float[] R = new float[16];
            float[] I = new float[16];

            SensorManager.getRotationMatrix(R, I, acceleration_vals, 
                magnetic_vals);

            float[] orientation = new float[3];
            SensorManager.getOrientation(R, orientation);

            acceleration_vals = null;
            magnetic_vals = null;

            // Use the pitch to determine whether we are in ID mode or
            // conference mode.
            if (orientation[1] <= 0) {   // we're in conference mode
                this.setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                showScheduleView();
            } else {   // we're in ID mode
                this.setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                showBadgeView();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case BT_ENABLE_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    Log.d("=badge=", "Bluetooth successfully enabled");

                    // Connect to my laptop to test.
                    tryConnection();
                }
                else {
                    Log.d("=badge=", "ERROR: Could not enable Bluetooth.");
                }
                break;
        }
    }

    void tryConnection()
    {

        String mac = bt_adapter.getAddress();

        if(mac.equals(joe_mac)) {
            // Act as server.
            try {
                BluetoothServerSocket ss = bt_adapter.listenUsingRfcommWithServiceRecord("badge", BT_UUID);
                Log.d("=badge=", "Got server socket");
                BluetoothSocket s = ss.accept();
                Log.d("=badge=", "Accepted connection");
                
                ss.close();

                byte[] buffer = new byte[1024];
                int bytes;
                InputStream in = s.getInputStream();
                bytes = in.read(buffer);
                for (int i = 0; i < bytes; i++) {
                    Log.d("=badge=", "bytes[" + i + "]" + " = " + buffer[i]);
                }

                s.close();
                Log.d("=badge=", "Closed socket");
            }
            catch (IOException e) {
                Log.d("=badge=", "ERROR: IOException:" + e.toString());
            }
        }
        else {
            // Act as client.
            BluetoothDevice dev = bt_adapter.getRemoteDevice(joe_mac);
            Log.d("=badge=", "Attempting to connect");
            try {
                BluetoothSocket s = dev.createRfcommSocketToServiceRecord(BT_UUID);
                Log.d("=badge=", "Created socket");
                s.connect();
                Log.d("=badge=", "Returned from connect");

                byte[] bob = {1, 2, 3, 4};
                OutputStream out = s.getOutputStream();
                out.write(bob);
                Log.d("=badge=", "Sent message");

                s.close();
                Log.d("=badge=", "Closed socket");
            }
            catch (IOException e) {
                Log.d("=badge=", "ERROR: IOException:" + e.toString());
            }
        }

    }

    void showBadgeView()
    {
        switcher.setDisplayedChild(0);
    }

    void showScheduleView() {
        switcher.setDisplayedChild(1);
    }
}
