package com.nclab.swimgamemulti.utils;

import android.util.Log;

public class Logger {
    private final static boolean LOGGING_ENABLED = true;
    private final static String TAG = "SwimGameMulti";

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

}