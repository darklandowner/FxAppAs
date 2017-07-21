package com.fx.app.util;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class VolleyHttpUtil {
	private static String TAG = VolleyHttpUtil.class.getName();

	private static RequestQueue mQueue = null;

	public static void init(Context context) {
		mQueue = Volley.newRequestQueue(context);
		Log.i(TAG, "init VolleyHttpUtil");
	}

	public static <T> Request<T> add(Request<T> request) {
		return mQueue == null ? null : mQueue.add(request);
	}

}
