package com.jperla.badge;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.widget.TextView;

public class BadgeActivity extends Activity implements SensorEventListener
{

    TextView tv;
    SensorManager sm;
    Sensor acc_sensor;
    Sensor mag_sensor;
    float[] acceleration_vals = null;
    float[] magnetic_vals = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Create a text view.
        tv = new TextView(this);
        tv.setText("");
        setContentView(tv);

        // Fetch the sensor manager.
        Context c = tv.getContext();
        sm = (SensorManager) c.getSystemService(SENSOR_SERVICE);

        // Register a listener for the accelerometer.
        acc_sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Register a listener for the magnetic field sensor.
        mag_sensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sm.registerListener(this, mag_sensor, SensorManager.SENSOR_DELAY_NORMAL);

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
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
    }

    public void onSensorChanged(SensorEvent event)
    {

        // Get the data from the sensor.
        switch (event.sensor.getType())
        {
            case Sensor.TYPE_ACCELEROMETER:
                acceleration_vals = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetic_vals = event.values.clone();
                break;
        }

        // If we have all the necessary data, update the orientation.
        if(acceleration_vals != null && magnetic_vals != null)
        {
            float[] R = new float[16];
            float[] I = new float[16];

            SensorManager.getRotationMatrix(R, I, acceleration_vals, magnetic_vals);

            float[] orientation = new float[3];
            SensorManager.getOrientation(R, orientation);
            tv.setText("Azimuth: " + orientation[0] + "\n" +
                       "Pitch: "   + orientation[1] + "\n" +
                       "Roll: "    + orientation[2] + "\n");

            acceleration_vals = null;
            magnetic_vals = null;
        }
    }

}
