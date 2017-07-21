package com.fx.app.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.fx.app.util.udid.OpenUDIDManage;

public class SysUtils {
	private static String tag = SysUtils.class.getName();

	public String getUDID(Context context) {
		String androidid = DeviceInfo.androidid(context);
		if (androidid != null && androidid.length() > 0) {
			return androidid;
		}

		String udid = getImei(context);
		if (udid != null && udid.length() > 0) {
			return udid;
		}

		udid = getLocalMacAddress(context);
		if (udid != null && udid.length() > 0) {
			return udid;
		}

		return getOpenUDID(context);
	}

	private String getLocalMacAddress(Context context) {
		try {
			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			String mac = wifiInfo.getMacAddress();
			Log.i(tag, "get mac" + mac);
			return mac;
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(tag, e.toString());
		}
		return null;
	}

	private String getOpenUDID(Context context) {
		try {
			if (context != null) {
				OpenUDIDManage.sync(context);
				if (OpenUDIDManage.isInitialized()) {
					String udid = OpenUDIDManage.getOpenUDID();
					Log.i(tag, "get open udid" + udid);
					return udid;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(tag, e.toString());
		}
		return null;
	}

	private String getLocalIpAddress(Context context) {
		String result = null;
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface networkInterface = en.nextElement();
				for (Enumeration<InetAddress> enAddress = networkInterface.getInetAddresses(); enAddress
						.hasMoreElements();) {
					InetAddress inetAddress = enAddress.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						String ip = inetAddress.getHostAddress().toString();
						if (isMatches(ip) && !ip.contains("*")) {
							result = ip;
						}
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
			Log.e(tag, e.toString());
		}
		return result;
	}

	private String getImei(Context context) {
		try {
			TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String imei = tm.getDeviceId();
			Log.i(tag, "get imei" + imei);
			return imei;
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(tag, e.toString());
		}
		return null;
	}

	private boolean isMatches(String ip) {
		boolean flag = false;
		try {
			String regex = "^((\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5]|[*])\\.){3}(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5]|[*])$";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(ip);
			if (m.find()) {
				return true;
			} else {
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

}
