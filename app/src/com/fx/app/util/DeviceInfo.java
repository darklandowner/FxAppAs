package com.fx.app.util;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class DeviceInfo {

	public static String getMacAdress(Context context) {
		String macAdress = "";
		try {
			WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = wifi.getConnectionInfo();
			if (info != null) {
				String mac = info.getMacAddress();
				if (mac != null) {
					mac = mac.replace(":", "").replace(".", "");
					mac = mac.toUpperCase();
					macAdress = mac;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return macAdress;
	}

	public static String androidid(Context context) {
		String rtnstr = "";
		try {
			rtnstr = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtnstr;
	}

	public static String deviceid(Context context) {
		String rtnstr = "";
		try {
			TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			rtnstr = tm.getDeviceId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtnstr;
	}

	public static int ip(Context context) {
		int rtnstr =0;
		try {

			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			rtnstr =wifiInfo.getIpAddress();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtnstr;
	}

	public static String brand() {
		String rtnstr = "";
		try {
			rtnstr = android.os.Build.BRAND;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtnstr;
	}

	public static String model() {
		String rtnstr = "";
		try {
			rtnstr = android.os.Build.MODEL;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtnstr;
	}

	public static String osversion() {
		String rtnstr = "";
		try {
			rtnstr = android.os.Build.VERSION.RELEASE;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtnstr;
	}
}
