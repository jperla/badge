package com.jperla.badge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothServerSocket;
import android.util.Log;
import java.util.UUID;
import java.io.IOException;
import android.os.Handler;

public class AcceptThread extends Thread {

    BluetoothAdapter bt_adapter;
    BluetoothServerSocket ss;
    Handler handler;

    public AcceptThread(BluetoothAdapter bt_adapter, Handler handler)
    {

        this.bt_adapter = bt_adapter;
        this.handler = handler;

        try {
            ss = bt_adapter.listenUsingRfcommWithServiceRecord("badge", Constants.BT_UUID);
            Log.d(Constants.LOG_TAG, "Got server socket");
        }
        catch (IOException e) {
            Log.d(Constants.LOG_TAG, "ERROR: " + e.toString());
        }
    }

    public void run()
    {
        BluetoothSocket s;
        while (true) {
            try {
                s = ss.accept();
                if (s != null) {
                    Log.d(Constants.LOG_TAG, "Accepted connection");
                    ss.close();
                
                    // Notify the application that the connection is established.
                    handler.obtainMessage(Constants.BT_CONN_ACCEPTED, 
                                          Constants.SUCCESS,
                                          -1, s).sendToTarget();
                    break;
                }
            }
            catch (IOException e) {
                Log.d(Constants.LOG_TAG, "ERROR: " + e.toString());
                handler.obtainMessage(Constants.BT_CONN_ACCEPTED, 
                                      Constants.FAILURE,
                                      -1, 0).sendToTarget();
                break;
            }
        }
    }
}
