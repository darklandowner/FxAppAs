package com.fx.app.thread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.fx.app.common.FxConst;
import com.fx.app.util.VolleyHttpUtil;

public class DownloadApkThread implements Runnable {
	private static String TAG = DownloadApkThread.class.getName();

	private static boolean cancelUpdate = false;
	private Context context;
	private Handler handler;

	public DownloadApkThread(Handler handler, Context context) {
		this.handler = handler;
		this.context = context;
		cancelUpdate = false;
	}

	public static void stop() {
		cancelUpdate = true;
	}

	@Override
	public void run() {
		try {
			// 判断SD卡是否存在，并且是否具有读写权限
			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
					|| Environment.getExternalStorageDirectory().getFreeSpace() < FxConst.xj_file_size) {
				handler.sendEmptyMessage(FxConst.HIDE_LOADING);
				handler.sendEmptyMessage(FxConst.STORAGE_ERROR);
				return;
			}

			// 获得存储卡的路径
			String mSavePath = Environment.getExternalStorageDirectory() + "/download";
			File file = new File(mSavePath);
			// 判断文件目录是否存在
			if (!file.exists()) {
				if (!file.mkdir()) {
					sendErrorMsg();
					return;
				}
				file.setWritable(true);
			}

			StringRequest stringRequest = new StringRequest(FxConst.down_url, new Response.Listener<String>() {
				@Override
				public void onResponse(String response) {
					Log.d(TAG, response);
					try {
						if (response != null) {
							int progress = 0;

							SharedPreferences sp = context.getSharedPreferences("hjLauncher", Context.MODE_PRIVATE);
							String url = null;
							if (sp != null) {
								url = sp.getString("downUrl", null);
							}

							JSONObject retj = new JSONObject(response);
							if (retj != null && "200".equals(retj.optString("status")) && retj.has("url")) {
								String newUrl = retj.optString("url");
								// test
								// url = "http://imgcn.ufile.ucloud.cn/cn_xjqxz_1_0_3_cps1420.apk";
								// url = "http://apkbj.ufile.ucloud.com.cn/xj/xianjian0719_GAEA_pack_signed_Aligned.apk";
								// url = "http://imgcn.ufile.ucloud.com.cn/xj/GaeaBasaraProject-XJBOX-release.apk";
								// url = "http://imgcn.cdn.bcebos.com/cn_xjqxz/1.0.3/cn_xjqxz_1_0_3_cps1106.apk";

								if (newUrl == null) {
									sendErrorMsg();
									return;
								}

								String mSavePath = Environment.getExternalStorageDirectory() + "/download";
								File apkFile = new File(mSavePath, FxConst.xj_file_name);
								if (apkFile.exists()) {
									if (newUrl.equalsIgnoreCase(url)) {
										// 下载完成
										handler.sendEmptyMessage(FxConst.DOWNLOAD_FINISH);
										return;
									} else {
										apkFile.delete();
									}
								}

								HttpURLConnection conn = (HttpURLConnection) new URL(newUrl).openConnection();
								// check part file
								long count = 0;
								File apkpartFile = new File(mSavePath, FxConst.xj_file_name + "part");
								if (apkpartFile.exists()) {
									if (apkpartFile.lastModified() + 24 * 60 * 60 * 1000 > System.currentTimeMillis()) {
										if (newUrl.equalsIgnoreCase(url)) {
											// 获取最终下载链接
											conn.setRequestMethod("HEAD");
											conn.setInstanceFollowRedirects(true);
											conn.setConnectTimeout(15 * 1000);
											conn.setReadTimeout(15 * 1000);

											int code = conn.getResponseCode();
											Log.i(TAG, "http code:" + code);
											if (code < 0) {
												sendErrorMsg();
												return;
											}

											url = conn.getURL().toString();
											Log.i(TAG, "redirect to:" + url);
											// for (Entry<String, List<String>> en : conn.getHeaderFields().entrySet()) {
											// Log.i(TAG, en.getKey() + ":" + en.getValue().get(0));
											// }
											conn.disconnect();

											// 断点续传
											conn = (HttpURLConnection) new URL(url).openConnection();
											count = apkpartFile.length();
											Log.i(TAG, "apk part size:" + count);
											conn.setRequestProperty("Range", "bytes=" + count + "-");
										} else {
											Log.i(TAG, "apk part is changed:" + newUrl);
											apkpartFile.delete();
										}
									} else {
										Log.i(TAG,
												"apk part is too old:"
														+ new Date(apkpartFile.lastModified()).toString());
										apkpartFile.delete();
									}
								}

								if (sp != null) {
									SharedPreferences.Editor editor = sp.edit();
									if (editor != null) {
										editor.putString("downUrl", newUrl).commit();
									}
								}

								// begin download
								conn.setInstanceFollowRedirects(true);
								conn.setConnectTimeout(15 * 1000);
								conn.setReadTimeout(15 * 1000);
								int code = conn.getResponseCode();
								Log.i(TAG, "http code:" + code);
								if (code < 0) {
									sendErrorMsg();
									return;
								}

								Log.i(TAG, "Content-Range:" + conn.getHeaderField("Content-Range"));
								if (count > 0 && conn.getHeaderField("Content-Range") == null) {
									Log.i(TAG, "not support Content-Range:" + url);
									apkpartFile.delete();
									sendErrorMsg();
									return;
								}

								// 获取文件大小
								long length = conn.getContentLength();
								Log.i(TAG, "content size:" + length);
								length += count;

								InputStream is = conn.getInputStream();
								FileOutputStream fos = new FileOutputStream(apkpartFile, true);
								byte buf[] = new byte[32 * 1024];
								do {
									int numread = is.read(buf);
									count += numread > 0 ? numread : 0;

									// 更新进度
									int progressNew = (int) (((float) count / length) * 100);
									if (progressNew > progress) {
										progress = progressNew;
										Bundle data = new Bundle();
										data.putInt("progress", progressNew);
										Message msg = new Message();
										msg.what = FxConst.REFRESH_DOWNLOAD_PROGRESS;
										msg.setData(data);
										handler.sendMessage(msg);
									}

									if (numread <= 0) {
										fos.close();
										is.close();
										Log.i(TAG, "download size:" + count);
										if (length > 0 && count != length) {
											sendErrorMsg();
										} else {
											// 下载完成
											handler.sendEmptyMessage(FxConst.DOWNLOAD_FINISH);
										}
										break;
									}
									// 写入文件
									fos.write(buf, 0, numread);
								} while (!cancelUpdate);// 点击取消就停止下载.
								conn.disconnect();
							}
						}
					} catch (Exception e) {
						Log.e(TAG, e.toString(), e);
						sendErrorMsg();
					}
				}
			}, new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					Log.e(TAG, error.getMessage(), error);
					sendErrorMsg();
				}
			});

			VolleyHttpUtil.add(stringRequest);
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
			sendErrorMsg();
		}
		handler.sendEmptyMessage(FxConst.HIDE_LOADING);
	}

	private void sendErrorMsg() {
		handler.sendEmptyMessage(FxConst.HIDE_LOADING);
		handler.sendEmptyMessage(FxConst.DOWNLOAD_ERROR);
	}

}
