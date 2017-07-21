package com.fx.app;

import java.util.Calendar;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

public class FxReceiver extends BroadcastReceiver {
	protected static final String TAG = FxReceiver.class.getName();

	public FxReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive:" + new Date());

		Calendar cd = Calendar.getInstance();
		cd.set(Calendar.HOUR_OF_DAY, 19);
		cd.set(Calendar.MINUTE, 50);
		cd.set(Calendar.SECOND, 0);

		if (System.currentTimeMillis() > cd.getTimeInMillis()) {
			cd.set(Calendar.HOUR_OF_DAY, 20);
			cd.set(Calendar.MINUTE, 20);

			if (System.currentTimeMillis() < cd.getTimeInMillis()) {
				String title = context.getResources().getString(R.string.noticeTitle);
				SpannableStringBuilder ssb = null;
				if (MainActivity.isAppInstalled(context)) {
					ssb = new SpannableStringBuilder(context.getResources().getString(R.string.updateLauncher));
				} else {
					String msg = context.getResources().getString(R.string.noticeMsg1);
					ssb = new SpannableStringBuilder(msg);
					ssb.append(context.getResources().getString(R.string.noticeMsg2));
					ssb.setSpan(new ForegroundColorSpan(Color.GREEN), 0, msg.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
					ssb.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, msg.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
				}

				// 1.实例化一个通知，指定图标、概要、时间
				// Notification n = new Notification(R.drawable.app_icon, "通知", System.currentTimeMillis() + 60 * 1000);
				// n.setLatestEventInfo(this, title, ssb, pi);

				// 2.指定通知的标题、内容和intent
				PendingIntent pi = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);

				Notification n = new Notification.Builder(context).setAutoCancel(true).setContentTitle(title)
						.setContentText(ssb).setContentIntent(pi).setSmallIcon(R.drawable.app_icon)
						.setWhen(System.currentTimeMillis() + 60 * 1000).build();

				// 3.指定声音
				n.defaults = Notification.DEFAULT_SOUND;

				// 4.发送通知
				NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				nm.notify(1, n);
			}
		}
	}
}
