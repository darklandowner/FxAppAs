/**
 * 
 */
package com.fx.app.util.udid;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;

/**
 * UDID服务
 * 
 * @author shyboy(shgbog.shen@ifreeteam.com)
 * 
 */
public class OpenUDIDService extends Service {

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return new Binder() {
			@Override
			public boolean onTransact(int code, android.os.Parcel data,
					android.os.Parcel reply, int flags) {
				final SharedPreferences preferences = getSharedPreferences(
						OpenUDIDManage.PREFS_NAME, Context.MODE_PRIVATE);

				reply.writeInt(data.readInt()); // Return to the sender the
												// input random number
				reply.writeString(preferences.getString(
						OpenUDIDManage.PREF_KEY, null));
				return true;
			}
		};
	}

}
