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
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import java.io.IOException;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.os.Handler;
import android.os.Message;

public class BadgeActivity extends Activity implements SensorEventListener {
    ViewSwitcher switcher;
    CameraSurfaceView cam_surface;
    SensorManager sm;
    Sensor acc_sensor;
    Sensor mag_sensor;
    float[] acceleration_vals = null;
    float[] magnetic_vals = null;
    
    BluetoothAdapter bt_adapter;
    Handler handler;
    ConnectedThread open_connection = null;

    static final int BT_ENABLE_ACTIVITY = 1;
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
        cam_surface.imageView = (ImageView) findViewById(R.id.id_bitmap);

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
            Log.d(Constants.LOG_TAG, "ERROR: No Bluetooth adapter found");
        }
        else {
            Log.d(Constants.LOG_TAG, "Successfully found Bluetooth adapter");
        }

        Log.d(Constants.LOG_TAG, Constants.BT_UUID.toString());


        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;

                switch (what) {
                    case Constants.BT_CONN_ACCEPTED:
                        BluetoothSocket s = (BluetoothSocket) msg.obj;
                        handleAccepted(s);
                        break;
                    case Constants.BT_MSG_RCVD:
                        int bytes = msg.arg1;
                        byte[] buffer = (byte[]) msg.obj;
                        rcvMessage(bytes, buffer);
                        break;
                }

            }
        };
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
            Log.d(Constants.LOG_TAG, "Bluetooth already enabled");
            if (open_connection == null) {
                tryConnection();
            }
        }
        else {
            Log.d(Constants.LOG_TAG, "Requesting to enable Bluetooth");
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
                    Log.d(Constants.LOG_TAG, "Bluetooth successfully enabled");

                    tryConnection();
                }
                else {
                    Log.d(Constants.LOG_TAG, "ERROR: Could not enable Bluetooth.");
                }
                break;
        }
    }

    public void handleAccepted(BluetoothSocket s) {
        // Handle the case where this phone is acting as the server.
        open_connection = new ConnectedThread(s);
        open_connection.start();
    }

    public void handleConnected(BluetoothSocket s) {
        // Handle the case where this phone is acting as the client.
        open_connection = new ConnectedThread(s);
        open_connection.start();

        byte[] bob = {1, 2, 3, 4};
        open_connection.write(bob);
        Log.d(Constants.LOG_TAG, "Sent data");

        open_connection.close();
        open_connection = null;
    }

    public void rcvMessage(int bytes, byte[] buffer) {
        Log.d(Constants.LOG_TAG, "Received message");
        for (int i = 0; i < bytes; i++) {
            Log.d(Constants.LOG_TAG, "buffer[" + i + "] =" + buffer[i]);
        }
        open_connection.close();
        open_connection = null;
    }

    private class ConnectThread extends Thread {
        BluetoothDevice dev;
        BluetoothSocket s;

        public ConnectThread(BluetoothDevice device)
        {
            dev = device;
            try {
                s = dev.createRfcommSocketToServiceRecord(Constants.BT_UUID);
                Log.d(Constants.LOG_TAG, "Created socket");
            }
            catch (IOException e) {
                Log.d(Constants.LOG_TAG, "ERROR: " + e.toString());
            }
        }

        public void run()
        {
            try {
                Log.d(Constants.LOG_TAG, "Attempting to connect");
                s.connect();
                Log.d(Constants.LOG_TAG, "Connected to server");
            }
            catch (IOException e) {
                Log.d(Constants.LOG_TAG, "ERROR: " + e.toString());
                try { s.close(); } catch (IOException e2) { }
            }

            // Notify the application that the connection is established.
            handleConnected(s);
        }
    }

    private class ConnectedThread extends Thread {
        private BluetoothSocket s;
        private InputStream in;
        private OutputStream out;
 
        public ConnectedThread(BluetoothSocket socket)
        {
            s = socket;
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }
            catch (IOException e) {
                Log.d(Constants.LOG_TAG, "ERROR: " + e.toString());
            }
        }
 
        public void run()
        {
            byte[] buffer = new byte[1024];
            int bytes;
 
            // Read until we get an exception.
            while (true) {
                try {
                    bytes = in.read(buffer);
                    handler.obtainMessage(Constants.BT_MSG_RCVD, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    // Don't log this since it is expected to happen on connection close.
                    //Log.d(Constants.LOG_TAG, "ERROR: " + e.toString());
                    break;
                }
            }
        }
 
        public void write(byte[] bytes)
        {
            try {
                out.write(bytes);
            }
            catch (IOException e) {
                Log.d(Constants.LOG_TAG, "ERROR: " + e.toString());
            }
        }
 
        public void close()
        {
            try {
                s.close();
                Log.d(Constants.LOG_TAG, "Closed socket");
            } catch (IOException e) {
                Log.d(Constants.LOG_TAG, "ERROR: " + e.toString());
            }
        }

    }

    void tryConnection()
    {

        String mac = bt_adapter.getAddress();

        if(mac.equals(joe_mac)) {
            // Act as server.
            AcceptThread at = new AcceptThread(bt_adapter, handler);
            at.start();
        }
        else {
            // Act as client.
            BluetoothDevice dev = bt_adapter.getRemoteDevice(joe_mac);
            ConnectThread ct = new ConnectThread(dev);
            ct.start();
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
