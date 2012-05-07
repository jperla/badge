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
import android.widget.ViewSwitcher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.UUID;

import com.qualcomm.QCARSamples.FrameMarkers.FrameMarkers;

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
    AcceptThread outstanding_accept = null;
    ConnectThread outstanding_connect = null;
    ConnectedThread open_connection = null;

    TreeMap<Integer, String> macs = new TreeMap<Integer, String>();

    static final int BT_ENABLE_ACTIVITY = 1;
    static final String joe_mac = "9C:02:98:70:23:67";
    static final String test_mac = "B0:D0:9C:38:8C:A2";

    VCard my_vcard;

    FrameMarkers fm;

    int msg_length_total = 0;
    int msg_length_cur = 0;
    byte[] cur_buffer = new byte[4096];

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

        if (bt_adapter.getAddress().equals(test_mac)) {
            Log.d(Constants.LOG_TAG, "Setting VCard to Zhao's");
            my_vcard = VCard.getZhao();
        }
        else {
            Log.d(Constants.LOG_TAG, "Settting VCard to Brandon's");
            my_vcard = VCard.getBrandon();
        }
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

    public void handleAccepted(BluetoothSocket s)
    {

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

        sendVCard(open_connection);
        Log.d(Constants.LOG_TAG, "Sent card data");
    }

    public void handleConnected(BluetoothSocket s)
    {

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

        sendVCard(open_connection);
        Log.d(Constants.LOG_TAG, "Sent card data");

    }

    public void sendVCard(ConnectedThread ct) 
    {
        String card_string = my_vcard.toJSON().toString();
        byte[] card_data;

        try {
            card_data = card_string.getBytes("ASCII");
        }
        catch (UnsupportedEncodingException e) {
            Log.d(Constants.LOG_TAG, "ERROR: Unsupported encoding");
            return;
        }

        Log.d(Constants.LOG_TAG, "Sending size " + card_data.length);
        Log.d(Constants.LOG_TAG, "Sending this v-card:");
        Log.d(Constants.LOG_TAG, card_string);

        // Send the message size
        ByteBuffer bb = ByteBuffer.allocate(4).putInt(card_data.length);
        Log.d(Constants.LOG_TAG, "SND BYTES: " + bb.get(0) + ", " + bb.get(1) + ", " + bb.get(2) + ", " + bb.get(3));
        byte[] size = bb.array();
        ct.write(size);

        // And then the message.
        ct.write(card_data);
    }

    public void rcvMessage(int bytes, byte[] buffer)
    {
        VCard their_card;

        ByteBuffer bb = ByteBuffer.wrap(buffer);

        // Fetch the bytes of this message.
        if (msg_length_total == 0) {
            Log.d(Constants.LOG_TAG, "RCV BYTES: " + bb.get(0) + ", " + bb.get(1) + ", " + bb.get(2) + ", " + bb.get(3));
            msg_length_total = bb.getInt();
            bb.get(cur_buffer, 0, bytes - 4);
            msg_length_cur += bytes - 4;
        }
        else {
            bb.get(cur_buffer, msg_length_cur, bytes);
            msg_length_cur += bytes;
        }

        Log.d(Constants.LOG_TAG, "Received " + bytes + " bytes (total " + msg_length_cur
                                 + " of " + msg_length_total + ")");

        String vcard_string;

        // Check if we have received the whole v-card.
        if (msg_length_cur == msg_length_total) {
            try {
                vcard_string = new String(
                    Arrays.copyOfRange(cur_buffer, 0, msg_length_cur), "ASCII");
                their_card = new VCard(vcard_string);
            }
            catch (UnsupportedEncodingException e) {
                Log.d(Constants.LOG_TAG, "ERROR: Unsupported encoding");
                open_connection.close();
                open_connection = null;
                startListening();
                return;
            }

            Log.d(Constants.LOG_TAG, "Received this string:");
            Log.d(Constants.LOG_TAG, vcard_string);

            Log.d(Constants.LOG_TAG, "Received this v-card:");
            Log.d(Constants.LOG_TAG, their_card.toJSON().toString());

            msg_length_cur = 0;
            msg_length_total = 0;

            open_connection.close();
            open_connection = null;
            startListening();

        }
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
        macs.put(0, test_mac);
        macs.put(1, joe_mac);
    }

}

