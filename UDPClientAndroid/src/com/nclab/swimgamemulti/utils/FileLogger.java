package com.nclab.swimgamemulti.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Environment;

public class FileLogger {
    private String rootDir = null;

    private BufferedWriter mLogWriter, mAccelWriter, mGyroWriter, mBTWriter;

    public FileLogger(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, "SG_Log");

        if (!file.exists()) {
            file.mkdirs();
        }
        rootDir = file.getAbsolutePath();


        String folderName = String.format("%s", getCurrentDateTime());
        File currentForder = new File(rootDir, folderName);
        currentForder.mkdir();
        try {
            this.close();

            mLogWriter = new BufferedWriter(new FileWriter(
                    currentForder.getAbsolutePath() + "/Log.txt"));
            mAccelWriter = new BufferedWriter(new FileWriter(
                    currentForder.getAbsolutePath() + "/Accelerometer.txt"));
            mGyroWriter = new BufferedWriter(new FileWriter(currentForder.getAbsolutePath()
                    + "/Gyroscope.txt"));
            mBTWriter = new BufferedWriter(new FileWriter(currentForder.getAbsolutePath()
                    + "/BTReceive.txt"));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            if (mAccelWriter != null) {
                mAccelWriter.flush();
                mAccelWriter.close();
            }

            if (mGyroWriter != null) {
                mGyroWriter.flush();
                mGyroWriter.close();
            }

            if (mLogWriter != null) {
                mLogWriter.flush();
                mLogWriter.close();
            }

            if (mBTWriter != null) {
                mBTWriter.flush();
                mBTWriter.close();
            }
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public void writeLog(String log){
        try {
            mLogWriter.write(log);
            mLogWriter.write("\r\n");
            mLogWriter.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeAccel(long time, float x, float y, float z){
        try {
            mAccelWriter.write(String.format("%d\t%f\t%f\t%f", time, x, y, z));
            mAccelWriter.write("\r\n");
            mAccelWriter.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeGyro(long time, float x, float y, float z){
        try {
            mGyroWriter.write(String.format("%d\t%f\t%f\t%f", time, x, y, z));
            mGyroWriter.write("\r\n");
            mGyroWriter.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getCurrentDateTime() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "yyyy_MM_dd_HH_mm_ss", Locale.KOREA);
        Date currentTime = new Date();
        String dTime = formatter.format(currentTime);
        return dTime;
    }
}
