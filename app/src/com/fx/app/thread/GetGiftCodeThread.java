package com.fx.app.thread;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.fx.app.MainActivity;
import com.fx.app.common.FxConst;
import com.fx.app.util.MD5Util;
import com.fx.app.util.VolleyHttpUtil;

public class GetGiftCodeThread implements Runnable {
	private static String TAG = GetGiftCodeThread.class.getName();

	private String udid;
	private Context context;
	private Handler handler;

	public GetGiftCodeThread(Handler handler, Context context, String udid) {
		this.handler = handler;
		this.context = context;
		this.udid = udid;
	}

	@Override
	public void run() {
		try {
			handler.sendEmptyMessage(FxConst.SHOW_LOADING);

			// Map<String, String> map = getPayOrderPara(udid, FxConst.hj_launcher_public_key);
			// String ret = MainActivity.httpPost(FxConst.gift_code_url, map, "utf-8");
			String sign = MD5Util.MD5(udid + FxConst.hj_launcher_public_key);
			// String ret = MainActivity.httpGet(FxConst.gift_code_url + "?udid=" + udid + "&sign=" + sign, "utf-8");
			StringRequest stringRequest = new StringRequest(FxConst.gift_code_url + "?udid=" + udid + "&sign=" + sign,
					new Response.Listener<String>() {
						@Override
						public void onResponse(String response) {
							Log.d(TAG, response);
							handler.sendEmptyMessage(FxConst.HIDE_LOADING);
							Message msg = new Message();
							msg.what = FxConst.GET_GIFTCODE_ERROR;
							Bundle bundle = new Bundle();
							try {
								if (response != null) {
									JSONObject retj = MainActivity.parseResult2Json(response);
									if (retj.has("giftCode")) {
										bundle.putString("giftCode", retj.optString("giftCode"));
										msg.what = FxConst.GET_GIFTCODE_FINISH;

										SharedPreferences sp = context.getSharedPreferences("hjLauncher",
												Context.MODE_PRIVATE);
										if (sp != null) {
											SharedPreferences.Editor editor = sp.edit();
											if (editor != null) {
												editor.putString("giftCode", retj.optString("giftCode")).commit();
											}
										}
									}
								}
							} catch (Exception e) {
								Log.e(TAG, e.toString(), e);
								handler.sendEmptyMessage(FxConst.HIDE_LOADING);
							}
							msg.setData(bundle);
							handler.sendMessage(msg);
						}
					}, new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							Log.e(TAG, error.getMessage(), error);
							handler.sendEmptyMessage(FxConst.HIDE_LOADING);
						}
					});
			VolleyHttpUtil.add(stringRequest);
		} catch (Exception e) {
			handler.sendEmptyMessage(FxConst.HIDE_LOADING);
			Log.e(TAG, e.toString(), e);
		}
	}

	// private Map<String, String> getPayOrderPara(String udid, String hjPublicKey) {
	// Map<String, String> map = new HashMap<String, String>();
	// map.put("udid", udid);
	//
	// String sign = MD5Util.MD5(udid + hjPublicKey);
	// if (sign == null) {
	// return null;
	// }
	// map.put("sign", sign);
	// return map;
	// }
}
