package com.nclab.swimgamemulti.motion;

import android.content.Context;
import android.util.Log;
import com.nclab.swimgamemulti.utils.FileLogger;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SwimmingMotionDetector {
    public static final int START_SWIMMING = 0;
    public static final int END_SWIMMING = 1;
    public static final int FREESTYLE_LEFT_STROKE = 2;
    public static final int FREESTYLE_RIGHT_STROKE = 3;
    public static final int BACKSTROKE_LEFT_STROKE = 4;
    public static final int BACKSTROKE_RIGHT_STROKE = 5;
    public static final int BREASTSTROKE_STROKE = 6;
    public static final int BUTTERFLY_STROKE = 7;
    public static final int STATUS_CHANGE = 8;

    public static final int TYPE_CHANGE = 9;
    public static final int NO_STROKE = 10;
    // public static final int TYPE_FREESTYLE = 5;

    public static final int STATUS_STAND = 0;
    public static final int STATUS_PRONE = 1;
    public static final int STATUS_LYING = 2;
    public static final int STATUS_CHANGING = 3;

    private static final int TYPE_NONE = 0;
    private static final int TYPE_FREESTYLE = 1;
    private static final int TYPE_BACKSTROKE = 2;
    private static final int TYPE_BREASTSTROKE = 3;
    private static final int TYPE_BUTTERFLY = 4;

    private static final String TAG = "StrokeDetector";
    private static final long STATUS_CHECK_TIME = 1000000000;    //1sec
    private static final long STROKE_CHECK_TIME = 1200000000;    //1.5sec
    private static final long STROKE_DETAIL_CHECK_TIME = 500000000;    //0.5sec

    private SensorDataManager mSensorDataManager;
    private FileLogger mLogger;

    public SwimmingMotionDetector(Context ctx, FileLogger logger) {
        mSensorDataManager = new SensorDataManager(ctx, this);
        mLogger = logger;
    }

    public void close() {
        mSensorDataManager.close();
        mSensorDataManager = null;
        mLogger = null;
    }

    // private int
    private static final int WINDOW_SIZE = 300;    //3sec

    private float[][] accel_window = new float[WINDOW_SIZE][3];
    private float[][] gyro_window = new float[WINDOW_SIZE][3];
    private long[] accel_time_window = new long[WINDOW_SIZE];
    private long[] gyro_time_window = new long[WINDOW_SIZE];
    private int accel_window_index = 0;
    private int gyro_window_index = 0;


    private int current_status = STATUS_STAND;
    private int current_type = TYPE_NONE;
    private long last_stroke_time = 0;
    private Lock data_lock = new ReentrantLock();


    private int current_stroke;

    public int getStroke() {
        return current_stroke;
    }

    public int getStatus() {
        return current_status;
    }

    public void update(long time) {
        processingData();
    }

    private boolean isEnoughTimeElapsed(int interval) {
        return System.currentTimeMillis() - last_stroke_time > interval;
    }


    boolean free_left_stroke = false, free_right_stroke = false, back_left_stroke = false, back_right_stroke = false;

    private void processingData() {
        // status processing
        int new_status = STATUS_CHANGING;
        int count_stand = 0, count_prone = 0, count_lying = 0, count = 0;

        int loacl_accel_index = 0;
        int loacl_gyro_index = 0;
        float[] accel_data = {0, 0, 0};
        float[] gyro_data = {0, 0, 0};

        boolean recentlyHasXvalueMinus = false;
        boolean recentlyHasZvalueMoreThanMinusFive = false;
        boolean recentlyHasXvalueMoreThanSeven = false;

        data_lock.lock();
        try {
            loacl_accel_index = (accel_window_index + WINDOW_SIZE - 1) % WINDOW_SIZE;
            loacl_gyro_index = (gyro_window_index + WINDOW_SIZE - 1) % WINDOW_SIZE;
            accel_data = accel_window[loacl_accel_index];
            gyro_data = gyro_window[loacl_gyro_index];

            long last_accel_time = accel_time_window[loacl_accel_index];
            int index = loacl_accel_index;
            while (count < WINDOW_SIZE) {

                long time = accel_time_window[index];


                if (last_accel_time - time <= STATUS_CHECK_TIME) {

                    if (accel_window[index][0] > 4.5 && Math.abs(accel_window[index][2]) < 2)
                        ++count_stand;
                    else if (accel_window[index][2] > 2)
                        ++count_lying;
                    else if (accel_window[index][2] < -2)
                        ++count_prone;
                    ++count;
                }

                if (last_accel_time - time <= STROKE_DETAIL_CHECK_TIME) {
                    if (accel_window[index][0] > 7) {
                        recentlyHasXvalueMoreThanSeven = true;
                    }
                }

                index = (index + WINDOW_SIZE - 1) % WINDOW_SIZE;

                if (accel_window[index][0] < -2)
                    recentlyHasXvalueMinus = true;

                if (accel_window[index][2] > -4)
                    recentlyHasZvalueMoreThanMinusFive = true;

                if (last_accel_time - time > STROKE_CHECK_TIME)
                    break;
            }
        } finally {
            data_lock.unlock();
        }

        if (count_stand == count) {
            new_status = STATUS_STAND;
        } else if (count_prone == count) {
            new_status = STATUS_PRONE;
        } else if (count_lying == count) {
            new_status = STATUS_LYING;
        }

        if (current_status != new_status) {
            //if need change event do here
            if (new_status != STATUS_CHANGING)
                current_status = new_status;

        }

        String s = null;
        if (current_status == STATUS_STAND)
            s = "STATUS_STAND";
        else if (current_status == STATUS_PRONE)
            s = "STATUS_PRONE";
        else if (current_status == STATUS_LYING)
            s = "STATUS_LYING";
        else if (current_status == STATUS_CHANGING)
            s = "STATUS_CHANGE";

        //type processing
        if (current_status == STATUS_STAND) {
            current_type = TYPE_NONE;
            return;
        } else if (current_status == STATUS_LYING) {
            current_type = TYPE_BACKSTROKE;
        } else if (current_status == STATUS_PRONE) {
            //determine swimming style among freestyle, breaststroke, butterfly
            //3 sec
            current_type = BREASTSTROKE_STROKE;

        } else {
            //must not reach here
            current_type = TYPE_NONE;
            Log.d(TAG, "current swimming status has wrong value");
        }


        // stroke detecting

        current_stroke = NO_STROKE;
        if (current_status == STATUS_STAND) {
            current_type = TYPE_NONE;
            //return;
        } else if (current_status == STATUS_LYING) {
            current_type = TYPE_BACKSTROKE;
            //backstroke
            if (!back_left_stroke && accel_data[1] < -4.2) {
                //mHandler.obtainMessage(BACKSTROKE_LEFT_STROKE).sendToTarget();
                //current_stroke = BACKSTROKE_LEFT_STROKE;
                back_left_stroke = true;
            } else if (back_left_stroke && accel_data[1] > -4.2 && isEnoughTimeElapsed(500)) {
                current_stroke = BACKSTROKE_LEFT_STROKE;
                last_stroke_time = System.currentTimeMillis();
                back_left_stroke = false;
            } else if (!back_right_stroke && accel_data[1] > 4.2) {
                //mHandler.obtainMessage(BACKSTROKE_RIGHT_STROKE).sendToTarget();
                //current_stroke = BACKSTROKE_RIGHT_STROKE;
                back_right_stroke = true;
            } else if (back_right_stroke && accel_data[1] < 4.2 && isEnoughTimeElapsed(500)) {
                current_stroke = BACKSTROKE_RIGHT_STROKE;
                last_stroke_time = System.currentTimeMillis();
                back_right_stroke = false;
            }
        } else if (current_status == STATUS_PRONE) {
            //determine swimming style among freestyle, breaststroke, butterfly
            //3 sec
            current_type = BREASTSTROKE_STROKE;
            if (!free_left_stroke && accel_data[1] < -4.2) {
                //mHandler.obtainMessage(FREESTYLE_LEFT_STROKE).sendToTarget();
                //current_stroke = FREESTYLE_LEFT_STROKE;
                free_left_stroke = true;
            } else if (free_left_stroke && accel_data[1] > -4.2 && isEnoughTimeElapsed(500)) {
                current_stroke = FREESTYLE_LEFT_STROKE;
                last_stroke_time = System.currentTimeMillis();
                free_left_stroke = false;
            } else if (!free_right_stroke && accel_data[1] > 4.2) {
                //mHandler.obtainMessage(FREESTYLE_RIGHT_STROKE).sendToTarget();
                //current_stroke = FREESTYLE_RIGHT_STROKE;
                free_right_stroke = true;
            } else if (free_right_stroke && accel_data[1] < 4.2 && isEnoughTimeElapsed(500)) {
                //mHandler.obtainMessage(FREESTYLE_RIGHT_STROKE).sendToTarget();
                current_stroke = FREESTYLE_RIGHT_STROKE;
                last_stroke_time = System.currentTimeMillis();
                free_right_stroke = false;
            } else if (gyro_data[1] < -2.8 && recentlyHasXvalueMinus && recentlyHasZvalueMoreThanMinusFive && Math.abs(accel_data[1]) < 3 && isEnoughTimeElapsed(800)) {
                //mHandler.obtainMessage(BREASTSTROKE_STROKE).sendToTarget();
                current_stroke = BUTTERFLY_STROKE;
                last_stroke_time = System.currentTimeMillis();
            } else if (gyro_data[1] < -0.8 && !recentlyHasXvalueMinus && !recentlyHasZvalueMoreThanMinusFive && Math.abs(accel_data[1]) < 3 && isEnoughTimeElapsed(800)) {
                //mHandler.obtainMessage(BREASTSTROKE_STROKE).sendToTarget();
                current_stroke = BREASTSTROKE_STROKE;
                last_stroke_time = System.currentTimeMillis();
            }
        } else {
            //must not reach here
            current_type = TYPE_NONE;
            Log.d(TAG, "current swimming status has wrong value");
        }

        switch (current_type) {
            case TYPE_NONE: {

                break;
            }
            case TYPE_FREESTYLE: {
                break;
            }
            case TYPE_BACKSTROKE: {
                break;
            }
            case TYPE_BREASTSTROKE: {

                break;
            }
            case TYPE_BUTTERFLY: {
                break;
            }
        }
    }

    public synchronized void updateData(int type, long timestamp, float[] data) {
        data_lock.lock();
        try {
            switch (type) {
                case SensorDataManager.TYPE_ACCEL: {
                    accel_window[accel_window_index % WINDOW_SIZE] = data;
                    accel_time_window[accel_window_index % WINDOW_SIZE] = timestamp;
                    accel_window_index = (accel_window_index + 1) % WINDOW_SIZE;
                    break;
                }
                case SensorDataManager.TYPE_GYRO: {
                    gyro_window[gyro_window_index % WINDOW_SIZE] = data;
                    gyro_time_window[gyro_window_index % WINDOW_SIZE] = timestamp;
                    gyro_window_index = (gyro_window_index + 1) % WINDOW_SIZE;
                    break;
                }
            }
        } finally {
            data_lock.unlock();
        }
    }

    public void logAccel(long time, float x, float y, float z) {
        if (mLogger != null)
            mLogger.writeAccel(time, x, y, z);
    }

    public void logGyro(long time, float x, float y, float z) {
        if (mLogger != null)
            mLogger.writeGyro(time, x, y, z);
    }
}
