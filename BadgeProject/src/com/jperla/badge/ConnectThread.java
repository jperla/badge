package com.jperla.badge;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.util.UUID;
import java.io.IOException;
import android.os.Handler;


public class ConnectThread extends Thread {
    BluetoothDevice dev;
    BluetoothSocket s;
    Handler handler;

    public ConnectThread(BluetoothDevice device, Handler handler)
    {
        dev = device;
        this.handler = handler;

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
            handler.obtainMessage(Constants.BT_CONN_CONNECTED,
                                  Constants.FAILURE, -1, s).sendToTarget();

            return;
        }

        // Notify the application that the connection is established.
        handler.obtainMessage(Constants.BT_CONN_CONNECTED,
                              Constants.SUCCESS, -1, s).sendToTarget();
    }
}
