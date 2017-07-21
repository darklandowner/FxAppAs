/**
 * 
 */
package com.fx.app.util.udid;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.provider.Settings.Secure;

public class OpenUDIDManage implements ServiceConnection {

	public final static String PREF_KEY = "openudid";
	public final static String PREFS_NAME = "openudid_prefs";

	private static String openUDID = null;
	private static boolean mInitialized = false;

	private final Context mContext; // Application context
	private List<ResolveInfo> mMatchingIntents; // List of available OpenUDID
												// Intents
	private Map<String, Integer> mReceivedOpenUDIDs; // Map of OpenUDIDs found
														// so far

	private final SharedPreferences mPreferences; // 用于存储UDID信息
	private final Random mRandom;// 生成随机唯一数

	public OpenUDIDManage(Context context) {
		mPreferences = context.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);// 文件为私有的，只能被应用本身访问
		mContext = context;
		mRandom = new Random();
		mReceivedOpenUDIDs = new HashMap<String, Integer>();
	}

	/**
	 * 当服务建立时，调用该方法
	 */
	@Override
	public void onServiceConnected(ComponentName className, IBinder service) {
		try {
			Parcel data = Parcel.obtain();// 获取新的Parcel 数据
			data.writeInt(mRandom.nextInt());// 将生成的随机数写入到Parcel中
			Parcel reply = Parcel.obtain();// 获取响应的Parcel数据
			service.transact(1, Parcel.obtain(), reply, 0);// 调用IBinder对象，并返回信息
			if (data.readInt() == reply.readInt()) // 当读取的信息与接收的信息一致时
			{
				final String _openUDID = reply.readString();// 获取UDID值
				if (null != _openUDID) { // 当UDID不为空时
					if (mReceivedOpenUDIDs.containsKey(_openUDID))
						mReceivedOpenUDIDs.put(_openUDID,
								mReceivedOpenUDIDs.get(_openUDID) + 1);
					else
						mReceivedOpenUDIDs.put(_openUDID, 1);
				}
			}
		} catch (RemoteException e) {
		}
		mContext.unbindService(this);// 断开服务

		startService();
	}

	/**
	 * 当服务销毁时，调用该方法
	 */
	@Override
	public void onServiceDisconnected(ComponentName className) {
		mContext.unbindService(this);
	}

	/*
	 * 开启服务
	 */
	private void startService() {
		if (mMatchingIntents.size() > 0) { // 当有多个intent-filter时
			final ServiceInfo servInfo = mMatchingIntents.get(0).serviceInfo;// 检索指定的应用服务，从AndroidManifest.xml文件的service中获取信息
			final Intent intent = new Intent();
			intent.setComponent(new ComponentName(
					servInfo.applicationInfo.packageName, servInfo.name));
			mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
			mMatchingIntents.remove(0);
		} else {
			getMostFrequentOpenUDID(); // Choose the most frequent

			if (openUDID == null) // 当无法获取UDID时
				generateOpenUDID();// 生成新的OpenUDID
			storeOpenUDID();// 存储UDID到本地
			mInitialized = true;
		}
	}

	/**
	 * 获取UDID
	 */
	private void getMostFrequentOpenUDID() {
		if (mReceivedOpenUDIDs.isEmpty() == false) {
			final TreeMap<String, Integer> sorted_OpenUDIDS = new TreeMap<String, Integer>(
					new ValueComparator());
			sorted_OpenUDIDS.putAll(mReceivedOpenUDIDs);

			openUDID = sorted_OpenUDIDS.firstKey();
		}
	}

	/*
	 * 生成新的OpenUDID
	 */
	private void generateOpenUDID() {
		openUDID = Secure.getString(mContext.getContentResolver(),
				Secure.ANDROID_ID);// 在设备第一引导中获取ANDROID_ID
		if (openUDID == null || openUDID.equals("9774d56d682e549c")
				|| openUDID.length() < 15) {// 当ANDROID_ID为空，或者等于GalaxyTab通用的ANDROID_ID，或者不合法的
			final SecureRandom random = new SecureRandom();
			openUDID = new BigInteger(64, random).toString(16);// 生成UDID
		}
	}

	/**
	 * 存储UDID到本地
	 */
	private void storeOpenUDID() {
		final Editor editor = mPreferences.edit();
		editor.putString(PREF_KEY, openUDID);
		editor.commit();
	}

	/**
	 * 初始化OpenUDID
	 * 
	 * @param context
	 *            ：当前Activity
	 */
	public static void sync(Context context) {
		OpenUDIDManage manager = new OpenUDIDManage(context);

		openUDID = manager.mPreferences.getString(PREF_KEY, null);// 试着从本地配置文件获取OpenUDID的值，默认值为null
		if (openUDID == null) // 本地配置文件中没有OpenUDID
		{
			manager.mMatchingIntents = context.getPackageManager()
					.queryIntentServices(new Intent("org.OpenUDID.GETUDID"), 0);// 获取名称为org.OpenUDID.GETUDID的sevice

			if (manager.mMatchingIntents != null)
				manager.startService();// 开启服务

		} else {
			mInitialized = true;
		}
	}

	/**
	 * 判断是否初始化成功
	 * 
	 * @return boolean
	 */
	public static boolean isInitialized() {
		return mInitialized;
	}

	/**
	 * 获取openUDID的值
	 * 
	 * @return String
	 */
	public static String getOpenUDID() {
		return openUDID;
	}

	/**
	 * 将收集的UDID进行分类
	 * 
	 * @author shyboy(shgbog.shen@ifreeteam.com)
	 * 
	 */
	private class ValueComparator implements Comparator<Object> {
		public int compare(Object a, Object b) {
			if (mReceivedOpenUDIDs.get(a) < mReceivedOpenUDIDs.get(b)) {
				return 1;
			} else if (mReceivedOpenUDIDs.get(a) == mReceivedOpenUDIDs.get(b)) {
				return 0;
			} else {
				return -1;
			}
		}
	}

}
