package com.jperla.badge;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.util.UUID;
import java.io.IOException;
import android.os.Handler;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {
    BluetoothSocket s;
    InputStream in;
    OutputStream out;
    Handler handler;
 
    public ConnectedThread(BluetoothSocket socket, Handler handler)
    {
        s = socket;
        this.handler = handler;

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
                break;
            }
        }
    }
 
    public void write(byte[] bytes)
    {
        try {
            out.write(bytes);
            Log.d(Constants.LOG_TAG, "Sent " + bytes.length + " bytes of data");
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

