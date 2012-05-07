package com.jperla.badge;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.UUID;

import com.qualcomm.QCARSamples.FrameMarkers.FrameMarkers;
import com.jperla.badge.VCard;

public class BadgeActivity extends Activity implements SensorEventListener {
    ViewSwitcher switcher;
    TextView textView;
    CameraSurfaceView cam_surface;
    SensorManager sm;
    Sensor acc_sensor;
    Sensor mag_sensor;
    float[] acceleration_vals = null;
    float[] magnetic_vals = null;
    
    BluetoothAdapter bt_adapter;
    VCard my_vcard;
    Handler handler;
    AcceptThread outstanding_accept = null;
    ConnectThread outstanding_connect = null;
    ConnectedThread open_connection = null;

    TreeMap<Integer, String> macs = new TreeMap<Integer, String>();

    static final int BT_ENABLE_ACTIVITY = 1;
    
    static final String brandon_mac = "9C:02:98:70:23:67";
    static final String zhao_mac = "B0:D0:9C:38:8C:A2";

    FrameMarkers fm;

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
        textView = (TextView) findViewById(R.id.textView);

        // Create an area to preview face detection results, and fetch camera
//        cam_surface = (CameraSurfaceView) findViewById(R.id.surface_view);
//        cam_surface.imageView = (ImageView) findViewById(R.id.id_bitmap);

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
        if (bt_adapter == null) {
            Log.d(Constants.LOG_TAG, "ERROR: No Bluetooth adapter found");
        }
        else {
            Log.d(Constants.LOG_TAG, "Successfully found Bluetooth adapter");
        }

        // Fetch my VCard by looking up my mac address
        String my_mac = bt_adapter.getAddress();
        if (my_mac.equals(zhao_mac)) {
            my_vcard = VCard.getZhao();
        } else {
            my_vcard = VCard.getBrandon();
        }
        onReceiveOtherVCard(VCard.getZhao());

        Log.d(Constants.LOG_TAG, Constants.BT_UUID.toString());

        // Initialize the table of MAC addresses.
        initializeMACs();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
    
                int result;
                BluetoothSocket s;

                switch (what) {
                    case Constants.BT_CONN_ACCEPTED:
                        outstanding_accept = null;
                        result = msg.arg1;
                        Log.d(Constants.LOG_TAG, "Handler got BT_CONN_ACCEPTED: " + result);
                        if (result == Constants.SUCCESS) {
                            s = (BluetoothSocket) msg.obj;
                            handleAccepted(s);
                        }
                        break;
                    case Constants.BT_CONN_CONNECTED:
                        outstanding_connect = null;
                        result = msg.arg1;
                        Log.d(Constants.LOG_TAG, "Handler got BT_CONN_CONNECTED: " + result);
                        if (result == Constants.SUCCESS) {
                            s = (BluetoothSocket) msg.obj;
                            handleConnected(s);
                        }
                        break;
                    case Constants.BT_MSG_RCVD:
                        Log.d(Constants.LOG_TAG, "Handler got BT_MSG_RCVD");
                        int bytes = msg.arg1;
                        byte[] buffer = (byte[]) msg.obj;
                        rcvMessage(bytes, buffer);
                        break;
                    case Constants.PHONE_ID_DETECTED:
                        int phone_id = msg.arg1;
                        Log.d(Constants.LOG_TAG, "Handler got phone id: " + phone_id);
                        String mac = macs.get(phone_id);
                        if (mac == null) {
                            Log.d(Constants.LOG_TAG, "ERROR: Phone id does not map to MAC address");
                            break;
                        }
                        tryConnection(mac);
                        break;

                }

            }
        };

        fm = new FrameMarkers(savedInstanceState, c, this, handler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        fm.onPause();
        sm.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fm.onResume();

        sm.registerListener(this, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, mag_sensor, SensorManager.SENSOR_DELAY_NORMAL);

        if (bt_adapter.isEnabled()) {
            Log.d(Constants.LOG_TAG, "Bluetooth already enabled");
            startListening();
        }
        else {
            Log.d(Constants.LOG_TAG, "Requesting to enable Bluetooth");
            Intent enable_bt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable_bt, BT_ENABLE_ACTIVITY);
        }

    }

    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        fm.onConfigurationChanged(config);
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
            if (orientation[1] <= -0.2) {   // we're in conference mode
                this.setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                showScheduleView();
            } else if (orientation[1] >= 0.2) {   // we're in ID mode
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
                    startListening();
                }
                else {
                    Log.d(Constants.LOG_TAG, "ERROR: Could not enable Bluetooth.");
                }
                break;
        }
    }

    void startListening()
    {
        // 1 connection at a time
        if (outstanding_accept != null || open_connection != null) {
            return;
        }

        outstanding_accept = new AcceptThread(bt_adapter, handler);
        outstanding_accept.start();
    }

    void tryConnection(String mac)
    {

        // Act as client, reaching out to make a new connection.
        if (outstanding_connect != null || open_connection != null) {
            return;
        }

        Log.d(Constants.LOG_TAG, "Attempting to connect to " + mac);

        BluetoothDevice dev = bt_adapter.getRemoteDevice(mac);
        outstanding_connect = new ConnectThread(dev, handler);
        outstanding_connect.start();

    }

    public void handleAccepted(BluetoothSocket s) {

        if (open_connection != null) {
            Log.d(Constants.LOG_TAG, "ERROR: active connection in handleAccepted");
            try {
                s.close();
            } catch (IOException e) {}
            return;
        }

        // Handle the case where this phone is acting as the server.
        open_connection = new ConnectedThread(s, handler);
        open_connection.start();
    }

    public void handleConnected(BluetoothSocket s) {

        if (open_connection != null) {
            Log.d(Constants.LOG_TAG, "ERROR: active connection in handleConnected");
            try {
                s.close();
            } catch (IOException e) {}
            return;
        }

        // Handle the case where this phone is acting as the client.
        open_connection = new ConnectedThread(s, handler);
        open_connection.start();

        byte[] bob = {1, 2, 3, 4};
        open_connection.write(bob);
        Log.d(Constants.LOG_TAG, "Sent data");

        open_connection.close();
        open_connection = null;
        startListening();
    }

    public void rcvMessage(int bytes, byte[] buffer) {
        Log.d(Constants.LOG_TAG, "Received message");
        for (int i = 0; i < bytes; i++) {
            Log.d(Constants.LOG_TAG, "buffer[" + i + "] =" + buffer[i]);
        }
        open_connection.close();
        open_connection = null;
        startListening();
    }

    public void onReceiveOtherVCard(VCard other) {
        String common = VCard.extractCommonalities(my_vcard, other);
        textView.setText(common);
        textView.setTextSize(30);
    }

    void showBadgeView()
    {
        switcher.setDisplayedChild(0);
    }

    void showScheduleView() {
        switcher.setDisplayedChild(1);
    }

    protected void onDestroy() {
        super.onDestroy();
        fm.onDestroy();
    }

    // MAC addresses for our demo.
    void initializeMACs() {
        macs.put(0, zhao_mac);
        macs.put(1, brandon_mac);
    }

}

