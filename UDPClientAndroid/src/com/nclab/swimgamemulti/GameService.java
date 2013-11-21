package com.nclab.swimgamemulti;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

public class GameService extends Service {
	private static boolean isRunning = false;
	public static boolean IsRunning(){
		return isRunning;
	}

	// Service manage
	private final IBinder mBinder = new GameServiceBinder();
	
	public class GameServiceBinder extends Binder {
		GameService getService() {
			return GameService.this;
		}
	}

	private WakeLock wakeLock;
	private NotificationManager mNM;
	private static int NOTIFICATION_ID = R.string.app_name;
	
	private Game game;
	
	public Handler getGameHandler(){
		return game.getGameHandler();
	}
	
	public void setUIHandler(Handler h){
		game.setUIHandler(h);
	}

	
	@Override
	public void onCreate() {
		// The service is being created
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();
		isRunning = true;
		if(wakeLock == null){
			PowerManager mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
			WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GameService");
			wakeLock.acquire();
		}
		
		if(game == null){
			game = new Game(this);
			Thread t = new Thread(game);
			t.setPriority(Thread.MAX_PRIORITY);
			t.start();
		}
	}

	@Override
	public void onDestroy() {
		// The service is no longer used and is being destroyed
		Toast.makeText(this, "service stopped", Toast.LENGTH_SHORT).show();

		mNM.cancel(NOTIFICATION_ID);
		isRunning = false;
		if(wakeLock != null){
			wakeLock.release();
			wakeLock = null;
		}
		
		if(game != null){
			game.exit();
			game = null;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// The service is starting, due to a call to startService()
		// return mStartMode;
		return Service.START_NOT_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
		//return null;
	}

	private void showNotification() {
//		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//				new Intent(this, GameActivity.class), 0);
//
//		Notification notification = new NotificationCompat.Builder(this)
//				.setContentTitle("SwimmingGame Running...")
//				.setContentText("bulabula")
//				.setSmallIcon(R.drawable.ic_launcher)
//				.setContentIntent(contentIntent).build();
//
//		mNM.notify(NOTIFICATION_ID, notification);
	}
}
