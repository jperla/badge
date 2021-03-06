package com.jperla.badge;

import java.util.UUID;

public final class Constants  {

    // Tag for debug logging
    public static final String LOG_TAG = "=badge=";

    // UUID for bluetooth app
    public static final UUID BT_UUID = UUID.fromString("ff15e609-34b1-4b49-9d40-a650566c8960");

    // Display timeout in seconds.
    public static final double DISPLAY_TIMEOUT = 10;

    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;

    public static final int BT_CONN_ACCEPTED = 1;
    public static final int BT_CONN_CONNECTED = 2;
    public static final int BT_MSG_RCVD = 3;
    public static final int PHONE_ID_DETECTED = 4;
    public static final int CLEAR_SCREEN = 5;

    private Constants() {
    }
}
