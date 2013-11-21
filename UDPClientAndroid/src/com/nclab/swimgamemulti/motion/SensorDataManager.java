package com.nclab.swimgamemulti.motion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorDataManager implements SensorEventListener {

	public static final int TYPE_ACCEL = 0;
	public static final int TYPE_GYRO = 1;
	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer, mGyroscope;
	
	private static final int WINDOW_SIZE = 20;
	private static final long WINDOW_TIME = 100000000; 	//0.1 sec
	
	private float[][] acceleration_window = new float[WINDOW_SIZE][3];
	private float[][] gyroscope_window = new float[WINDOW_SIZE][3];
	private int acceleration_window_index = 0;
	private int gyroscope_window_index = 0;
	
	
	private SwimmingMotionDetector mSwimmingMotionDetector;
	
	public SensorDataManager(Context ctx, SwimmingMotionDetector manager){
		mSwimmingMotionDetector = manager;
		mSensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		
		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mGyroscope,
				SensorManager.SENSOR_DELAY_GAME);
	}
	
	
	public void close(){
		mSensorManager.unregisterListener(this, mAccelerometer);
		mSensorManager.unregisterListener(this, mGyroscope);
		mSensorManager = null;
	}
	

	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	private long first_accel_time, first_gyro_time = 0;
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		Sensor sensor = event.sensor;
		
		if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			
			if(acceleration_window_index == 0){
				first_accel_time = event.timestamp;
			}
		
			for(int i=0; i<3; ++i){
				acceleration_window[acceleration_window_index][i] = event.values[i];
			}
			++acceleration_window_index;
			if(acceleration_window_index >= WINDOW_SIZE || (event.timestamp - first_accel_time > WINDOW_TIME && acceleration_window_index != 0)){
				//calculate
				float[] avg = {0f,0f,0f};  
				for(int i=0; i<acceleration_window_index; ++i){
					for(int j=0; j<3; ++j){
						avg[j] += acceleration_window[i][j];
					}						
				}
				for(int i=0; i<3; ++i){
					avg[i] /= (float)acceleration_window_index;
				}
				
				acceleration_window_index = 0;
				mSwimmingMotionDetector.updateData(TYPE_ACCEL, event.timestamp, avg);
			}
			mSwimmingMotionDetector.logAccel(event.timestamp, event.values[0], event.values[1], event.values[2]);
			
			//mSwimmingMotionDetector.updateData(TYPE_ACCEL, event.timestamp, event.values);
			
			// TODO: get values
			/*
			for(int i=0; i<3; ++i){
				acceleration_window[acceleration_window_index][i] = event.values[i];
			}
			++acceleration_window_index;
			if(acceleration_window_index >= WINDOW_SIZE){
				acceleration_window_index = 0;
				//calculate
				float[] avg = {0f,0f,0f};  
				for(int i=0; i<WINDOW_SIZE; ++i){
					for(int j=0; j<3; ++j){
						avg[j] += acceleration_window[i][j];
					}						
				}
				for(int i=0; i<3; ++i){
					avg[i] /= (float)WINDOW_SIZE;
				}
				//mHandler.obtainMessage(TYPE_ACCEL, avg).sendToTarget();
				mSwimmingMotionDetector.updateData(TYPE_ACCEL, avg);
			}*/
		} else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			
			if(gyroscope_window_index == 0){
				first_gyro_time = event.timestamp;
			}
		
			for(int i=0; i<3; ++i){
				gyroscope_window[gyroscope_window_index][i] = event.values[i];
			}
			++gyroscope_window_index;
			if(gyroscope_window_index >= WINDOW_SIZE || (event.timestamp - first_gyro_time > WINDOW_TIME && gyroscope_window_index != 0)){
				//calculate
				float[] avg = {0f,0f,0f};  
				for(int i=0; i<gyroscope_window_index; ++i){
					for(int j=0; j<3; ++j){
						avg[j] += gyroscope_window[i][j];
					}						
				}
				for(int i=0; i<3; ++i){
					avg[i] /= (float)gyroscope_window_index;
				}
				
				gyroscope_window_index = 0;
				mSwimmingMotionDetector.updateData(TYPE_GYRO, event.timestamp, avg);
			}
			mSwimmingMotionDetector.logGyro(event.timestamp, event.values[0], event.values[1], event.values[2]);
			//mSwimmingMotionDetector.updateData(TYPE_GYRO, event.timestamp, event.values);
			
			// TODO: get values
			/*
			for(int i=0; i<3; ++i){
				gyroscope_window[gyroscope_window_index][i] = event.values[i];
			}
			++gyroscope_window_index;
			if(gyroscope_window_index >= WINDOW_SIZE){
				gyroscope_window_index = 0;
				//calculate
				float[] avg = {0f,0f,0f};  
				for(int i=0; i<WINDOW_SIZE; ++i){
					for(int j=0; j<3; ++j){
						avg[j] += gyroscope_window[i][j];
					}						
				}
				for(int i=0; i<3; ++i){
					avg[i] /= (float)WINDOW_SIZE;
				}
				//mHandler.obtainMessage(TYPE_GYRO, avg).sendToTarget();\
				mSwimmingMotionDetector.updateData(TYPE_GYRO, avg);
			}*/
		}
	}

}
