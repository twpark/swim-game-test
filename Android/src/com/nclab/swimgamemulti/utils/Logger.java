package com.nclab.swimgamemulti.utils;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;

public class Logger {
    private final static boolean LOGGING_ENABLED = true;
    private final static String TAG = "SwimGameMulti";

    private static BufferedWriter accelWriter;

    public static void initialize() {
    }

    public static void d(String msg) {
        if (LOGGING_ENABLED)
            Log.d(TAG, msg);
    }

    public static void d(String msg, Throwable t) {
        if (LOGGING_ENABLED)
            Log.d(TAG, msg, t);
    }

            public static void e(String msg) {
        if (LOGGING_ENABLED)
            Log.e(TAG, msg);
    }

    public static void e(String msg, Throwable t) {
        if (LOGGING_ENABLED)
            Log.e(TAG, msg, t);
    }

    public static void i(String msg) {
        if (LOGGING_ENABLED)
            Log.i(TAG, msg);
    }

    public static void i(String msg, Throwable t) {
        if (LOGGING_ENABLED)
            Log.i(TAG, msg, t);
    }

    public static void v(String msg) {
        if (LOGGING_ENABLED)
            Log.v(TAG, msg);
    }

    public static void v(String msg, Throwable t) {
        if (LOGGING_ENABLED)
            Log.v(TAG, msg, t);
    }

    public static void w(String msg) {
        if (LOGGING_ENABLED)
            Log.w(TAG, msg);
    }

    public static void w(String msg, Throwable t) {
        if (LOGGING_ENABLED)
            Log.w(TAG, msg, t);
    }

    public static void accel(long time, float x, float y, float z) {
        if (LOGGING_ENABLED) {
            try {
                accelWriter.write(String.format("%d\t%f\t%f\t%f", time, x, y, z));
                accelWriter.write("\r\n");
                accelWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // byte[] to hex
    public static String byteArrayToHex(byte[] ba) {
        if (ba == null || ba.length == 0) {
            return null;
        }

        StringBuffer sb = new StringBuffer(ba.length * 2);
        String hexNumber;
        for (int x = 0; x < ba.length; x++) {
            hexNumber = "0" + Integer.toHexString(0xff & ba[x]);

            sb.append(hexNumber.substring(hexNumber.length() - 2));
        }
        return sb.toString();
    }
}