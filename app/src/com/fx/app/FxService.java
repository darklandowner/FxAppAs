package com.fx.app;

import java.util.Calendar;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class FxService extends Service {
	private static String TAG = FxService.class.getName();

	private boolean quit = false;
	private Binder serviceBinder = new Binder();

	@Override
	public IBinder onBind(Intent intent) {
		return serviceBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (!quit) {
						Calendar cd = Calendar.getInstance();
						if (cd.get(Calendar.HOUR_OF_DAY) == 17 && cd.get(Calendar.MINUTE) == 0) {
							Log.i(TAG, "service send msg at" + cd.getTime());
							Intent intent = new Intent("ELITOR_CLOCK");
							sendBroadcast(intent);
						}
						Thread.sleep(60000);
					}
				} catch (Exception e) {
					Log.e(TAG, e.toString(), e);
				}
			}
		}).start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.quit = true;
	}

}
