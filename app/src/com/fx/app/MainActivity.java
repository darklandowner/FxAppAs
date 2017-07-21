package com.fx.app;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fx.app.common.FxConst;
import com.fx.app.thread.DownloadApkThread;
import com.fx.app.thread.GetGiftCodeThread;
import com.fx.app.thread.UpdateApkThread;
import com.fx.app.util.SysUtils;
import com.fx.app.util.VolleyHttpUtil;

public class MainActivity extends Activity implements OnClickListener, OnTouchListener {
	protected static final String TAG = MainActivity.class.getName();

	private String udid = null;
	private static ProgressDialog loadingDialog = null;
	protected static ProgressDialog progressDialog = null;
	private ImageButton progressBtn, down, website, giftBtn, left, right;
	private Bitmap bm = null;
	private WebView webView;
	private ImageView root;
	private TextView textPercent;

	private float scalew, scaleh;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "msg.what:" + msg.what);
			Bundle data = msg.getData();
			switch (msg.what) {
				case FxConst.SHOW_LOADING:
					showLoading(MainActivity.this);
					break;
				case FxConst.HIDE_LOADING:
					hideLoading();
					break;
				case FxConst.DOWNLOAD_FINISH:
					finishDownload();
					break;
				case FxConst.UPDATE_FINISH:
					finishUpdate();
					break;
				case FxConst.REFRESH_DOWNLOAD_PROGRESS:
					int progress = data == null ? 0 : data.getInt("progress");
					if (progress > 0) {
						refreshProgress(progress);
					}
					break;
				case FxConst.REFRESH_UPDATE_PROGRESS:
					progress = data == null ? 0 : data.getInt("progress");
					if (progress > 0) {
						refreshUpdateProgress(progress);
					}
					break;
				case FxConst.DOWNLOAD_ERROR:
					showRedownload();
					break;
				case FxConst.UPDATE_ERROR:
					showReUpdate();
					break;
				case FxConst.STORAGE_ERROR:
					showStorageError();
					break;
				case FxConst.GET_GIFTCODE_FINISH:
					ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					clip.setText(data.getString("giftCode")); // 复制

					String str = getResources().getString(R.string.copyGift);
					str = String.format(str, data.getString("giftCode"));
					Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
					giftBtn.setClickable(true);
					break;
				case FxConst.GET_GIFTCODE_ERROR:
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle(android.R.string.dialog_alert_title).setMessage(R.string.retry)
							.setIcon(android.R.drawable.ic_dialog_info).setCancelable(false)
							.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									new Thread(new GetGiftCodeThread(handler, MainActivity.this, udid)).start();
								}
							}).setNegativeButton(android.R.string.cancel, null).show();
					giftBtn.setClickable(true);
					break;
				default:
					break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		VolleyHttpUtil.init(this);

		udid = new SysUtils().getUDID(this);

		WindowManager wm = this.getWindowManager();
		int width = wm.getDefaultDisplay().getWidth();
		int height = wm.getDefaultDisplay().getHeight();
		// width = 480;
		// height = 854;
		scalew = (float) width / 1080f;
		scaleh = (float) height / 1920f;
		Log.i(TAG, "width" + width);
		Log.i(TAG, "height" + height);
		Log.i(TAG, "scalew" + scalew);
		Log.i(TAG, "scaleh" + scaleh);

		root = (ImageView) findViewById(R.id.root);
		down = (ImageButton) findViewById(R.id.down);
		website = (ImageButton) findViewById(R.id.website);
		giftBtn = (ImageButton) findViewById(R.id.giftBtn);
		left = (ImageButton) findViewById(R.id.left);
		right = (ImageButton) findViewById(R.id.right);
		progressBtn = (ImageButton) findViewById(R.id.progress);
		textPercent = (TextView) findViewById(R.id.textPercent);
		webView = (WebView) findViewById(R.id.webView);

		root.setOnTouchListener(this);
		root.setClickable(true);

		// down.setScaleX(scalew);
		// down.setScaleY(scaleh);
		MarginLayoutParams margin = new LinearLayout.LayoutParams(down.getLayoutParams());
		margin.width = Math.round(margin.width * scalew);
		margin.height = Math.round(margin.height * scaleh);
		margin.leftMargin = Math.round(30 * scalew);
		margin.rightMargin = Math.round(30 * scalew);
		down.setLayoutParams(margin);
		down.setOnClickListener(this);

		// website.setScaleX(scalew);
		// website.setScaleY(scaleh);
		margin = new LinearLayout.LayoutParams(website.getLayoutParams());
		margin.width = Math.round(margin.width * scalew);
		margin.height = Math.round(margin.height * scaleh);
		website.setLayoutParams(margin);
		website.setOnClickListener(this);

		// giftBtn.setScaleX(scalew);
		// giftBtn.setScaleY(scaleh);
		margin = new LinearLayout.LayoutParams(giftBtn.getLayoutParams());
		margin.width = Math.round(margin.width * scalew);
		margin.height = Math.round(margin.height * scaleh);
		giftBtn.setLayoutParams(margin);
		if (udid == null || udid.length() < 1) {
			giftBtn.setVisibility(View.INVISIBLE);
		} else {
			giftBtn.setOnClickListener(this);
		}

		View btns = findViewById(R.id.btns);
		margin = new FrameLayout.LayoutParams(btns.getLayoutParams());
		margin.leftMargin = Math.round(width * 0.104f);
		margin.topMargin = Math.round(height * 0.709f);
		btns.setLayoutParams(margin);

		View lor = findViewById(R.id.lor);
		margin = new FrameLayout.LayoutParams(lor.getLayoutParams());
		margin.setMargins(margin.leftMargin, Math.round(height * 0.43f), margin.rightMargin, 0);
		lor.setLayoutParams(margin);

		left.setScaleX(scalew > 1 ? 1 : scalew);
		left.setScaleY(scaleh > 1 ? 1 : scaleh);
		left.setOnClickListener(this);

		right.setScaleX(scalew > 1 ? 1 : scalew);
		right.setScaleY(scaleh > 1 ? 1 : scaleh);
		right.setOnClickListener(this);

		// progressBtn.setScaleX(scalew);
		// progressBtn.setScaleY(scaleh);
		margin = new FrameLayout.LayoutParams(progressBtn.getLayoutParams());
		margin.setMargins(Math.round(width * 0.06f), Math.round(height * 0.845f), margin.rightMargin, 0);
		margin.width = Math.round(margin.width * scalew);
		margin.height = Math.round(margin.height * scaleh);
		progressBtn.setLayoutParams(margin);

		bm = BitmapFactory.decodeResource(getResources(), R.drawable.loading_02);
		bm = Bitmap.createScaledBitmap(bm, margin.width, margin.height, false);
		// refreshProgress(100); // test

		int textTop = Math.round(margin.topMargin * 1.035f);
		margin = new FrameLayout.LayoutParams(textPercent.getLayoutParams());
		margin.leftMargin = Math.round(width / 2.36f);
		margin.topMargin = textTop;
		textPercent.setLayoutParams(margin);

		PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent("ELITOR_CLOCK"), 0);
		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Calendar cd = Calendar.getInstance();
		cd.set(Calendar.HOUR_OF_DAY, 18);
		cd.set(Calendar.MINUTE, 0);
		cd.set(Calendar.SECOND, 0);
		// am.set(AlarmManager.RTC_WAKEUP, cd.getTimeInMillis(), pi);
		am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cd.getTimeInMillis(), 24 * 60 * 60 * 1000, pi);

		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(true);
		settings.setUseWideViewPort(true);// 关键点
		settings.setDisplayZoomControls(false);
		settings.setAllowFileAccess(true); // 允许访问文件
		settings.setBuiltInZoomControls(true); // 设置显示缩放按钮
		settings.setSupportZoom(true); // 支持缩放
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int mDensity = metrics.densityDpi;
		Log.d(TAG, "densityDpi = " + mDensity);
		if (mDensity == 240) {
			settings.setDefaultZoom(ZoomDensity.FAR);
		} else if (mDensity == 160) {
			settings.setDefaultZoom(ZoomDensity.MEDIUM);
		} else if (mDensity == 120) {
			settings.setDefaultZoom(ZoomDensity.CLOSE);
		} else if (mDensity == DisplayMetrics.DENSITY_XHIGH) {
			settings.setDefaultZoom(ZoomDensity.FAR);
		} else if (mDensity == DisplayMetrics.DENSITY_TV) {
			settings.setDefaultZoom(ZoomDensity.FAR);
		} else {
			settings.setDefaultZoom(ZoomDensity.MEDIUM);
		}

		webView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode == KeyEvent.KEYCODE_BACK) {
						if (webView.canGoBack()) {
							webView.goBack(); // 后退
						} else {
							webView.setVisibility(View.GONE);
						}
						return true; // 已处理
					}
				}
				return true;
			}
		});

		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});

		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
				return super.onJsAlert(view, url, message, result);
			}

			public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
				return super.onJsConfirm(view, url, message, result);
			}

			public boolean onJsPrompt(WebView view,
					String url,
					String message,
					String defaultValue,
					final JsPromptResult result) {
				return super.onJsPrompt(view, url, message, defaultValue, result);
			}
		});

		// 图片渐变模糊度始终
		AlphaAnimation aaa = new AlphaAnimation(0.5f, 1.0f);
		// 渐变时间
		aaa.setDuration(2000);
		aaa.setRepeatMode(AlphaAnimation.REVERSE);
		aaa.setRepeatCount(Animation.INFINITE);
		// 展示图片渐变动画
		left.startAnimation(aaa);
		// 展示图片渐变动画
		right.startAnimation(aaa);

		if (isAppInstalled(this)) {
			down.setBackgroundResource(R.drawable.button_lvup_01);
			down.setTag(FxConst.update_tag);
			giftBtn.setBackgroundResource(R.drawable.button_gift_01);
		} else {
			down.setBackgroundResource(R.drawable.button_start_01);
			down.setTag(FxConst.down_tag);

			SharedPreferences sp = getSharedPreferences("hjLauncher", Context.MODE_PRIVATE);
			String url = null;
			if (sp != null) {
				url = sp.getString("downUrl", null);
			}

			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				String mSavePath = Environment.getExternalStorageDirectory() + "/download";
				File apkFile = new File(mSavePath, FxConst.xj_file_name);
				if (apkFile.exists()) {
					if (url == null) {
						apkFile.delete();
					} else {
						refreshProgress(100);
						down.setBackgroundResource(R.drawable.continue_01);
						down.setTag(FxConst.finish_tag);
					}
				}
			}
		}

		// Intent intent = new Intent(getApplicationContext(), HjService.class);
		// startService(intent);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.down:
				down.setClickable(false);
				Object tag = down.getTag();
				if (!(tag instanceof Integer)) {
					tag = FxConst.down_tag;
				}
				switch ((Integer) tag) {
					case FxConst.down_tag:
						download();
						break;
					case FxConst.pause_tag:
						down.setBackgroundResource(R.drawable.button_start_01);
						down.setTag(FxConst.down_tag);
						down.setClickable(true);
						DownloadApkThread.stop();
						break;
					case FxConst.finish_tag:
						installApk();
						break;
					case FxConst.update_tag:
						updateLauncher();
						break;
					default:
						break;
				}
				break;
			case R.id.website:
				webView = (WebView) findViewById(R.id.webView);
				webView.setVisibility(View.VISIBLE);
				webView.loadUrl("http://xjqxz.gaeamobile.net/");
				webView.requestFocus();
				// startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://xjqxz.gaeamobile.net/")));
				// System.exit(0);
				break;
			case R.id.giftBtn:
				getGift();
				break;
			case R.id.left:
				left();
				break;
			case R.id.right:
				right();
				break;
			default:
				break;
		}

	}

	private float lastX = 0;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		Log.i(TAG, "Touch:" + action);
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				lastX = event.getRawX();
				break;
			case MotionEvent.ACTION_UP:
				if (lastX > event.getRawX()) {
					right();
				} else if (lastX < event.getRawX()) {
					left();
				}
				break;
		}
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	public static boolean isAppInstalled(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(FxConst.xj_pkg_name, 0);
			return packageInfo != null;
		} catch (NameNotFoundException e) {
			Log.e(TAG, e.toString(), e);
		}
		return false;
	}

	private static boolean isWifi(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI;
	}

	public static void showLoading(Context context) {
		// 创建ProgressDialog对象
		if (loadingDialog == null) {
			loadingDialog = new ProgressDialog(context);
			// 设置进度条风格，风格为圆形，旋转的
			loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			// 设置ProgressDialog 提示信息
			loadingDialog.setMessage("loading...");
			// 设置ProgressDialog 的进度条是否不明确
			loadingDialog.setIndeterminate(false);
			// 设置ProgressDialog 是否可以按退回按键取消
			loadingDialog.setCancelable(false);
		}
		if (loadingDialog != null) {
			// 让ProgressDialog显示
			loadingDialog.show();
		}
		timer();
	}

	public static void timer() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				hideLoading();
			}
		}, 5000);// 设定指定的时间time,此处为5000毫秒
	}

	public static void hideLoading() {
		if (loadingDialog != null) {
			loadingDialog.dismiss();
			loadingDialog = null;
		}
	}

	public static Boolean IsLoading() {
		if (loadingDialog != null) {
			return true;
		}
		return false;
	}

	private void download() {
		try {
			if (isWifi(this)) {
				down.setBackgroundResource(R.drawable.button_download_01);
				down.setTag(FxConst.pause_tag);
				down.setClickable(true);
				progressBtn.setImageBitmap(null);
				textPercent.setText("");
				new Thread(new DownloadApkThread(handler, MainActivity.this)).start();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(android.R.string.dialog_alert_title).setMessage(R.string.mobile)
						.setIcon(android.R.drawable.ic_dialog_info).setCancelable(false)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								down.setBackgroundResource(R.drawable.button_download_01);
								down.setTag(FxConst.pause_tag);
								progressBtn.setImageBitmap(null);
								textPercent.setText("");
								new Thread(new DownloadApkThread(handler, MainActivity.this)).start();
							}
						}).setNegativeButton(android.R.string.cancel, null).show();
				down.setClickable(true);
			}
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
		}
	}

	private void finishDownload() {
		String mSavePath = Environment.getExternalStorageDirectory() + "/download";
		// remove unfinish file
		File apkpartFile = new File(mSavePath, FxConst.xj_file_name + "part");
		if (apkpartFile.exists()) {
			File apkFile = new File(mSavePath, FxConst.xj_file_name);
			if (apkFile.exists()) {
				apkFile.delete();
			}
			apkpartFile.renameTo(apkFile);
		}

		if (udid == null || udid.length() < 1) {
			Log.i(TAG, "Can not get device ID.");
		} else {
			if (isBackground(this)) {
				String title = getResources().getString(R.string.noticeTitle);
				SpannableStringBuilder ssb = null;
				if (MainActivity.isAppInstalled(this)) {
					ssb = new SpannableStringBuilder(getResources().getString(R.string.updateLauncher));
				} else {
					String msg = getResources().getString(R.string.noticeMsg1);
					ssb = new SpannableStringBuilder(msg);
					ssb.append(getResources().getString(R.string.noticeMsg2));
					ssb.setSpan(new ForegroundColorSpan(Color.GREEN), 0, msg.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
					ssb.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, msg.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
				}

				// 1.实例化一个通知，指定图标、概要、时间
				// Notification n = new Notification(R.drawable.app_icon, "通知", System.currentTimeMillis() + 60 * 1000);
				// n.setLatestEventInfo(this, title, ssb, pi);

				// 2.指定通知的标题、内容和intent
				Intent intent = new Intent(this, MainActivity.class);
				PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

				Notification n = new Notification.Builder(this).setAutoCancel(true).setContentTitle(title)
						.setContentText(ssb).setContentIntent(pi).setSmallIcon(R.drawable.app_icon)
						.setWhen(System.currentTimeMillis() + 60 * 1000).build();

				// 3.指定声音
				n.defaults = Notification.DEFAULT_SOUND;

				// 4.发送通知
				NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				nm.notify(1, n);
			}
		}
		down.setBackgroundResource(R.drawable.continue_01);
		down.setTag(FxConst.finish_tag);
		down.setClickable(true);
		giftBtn.setBackgroundResource(R.drawable.button_gift_01);

		installApk();
	}

	private void finishUpdate() {
		String mSavePath = Environment.getExternalStorageDirectory() + "/download";
		// remove unfinish file
		File apkpartFile = new File(mSavePath, FxConst.baohe_file_name + "part");
		if (apkpartFile.exists()) {
			File apkFile = new File(mSavePath, FxConst.baohe_file_name);
			if (apkFile.exists()) {
				apkFile.delete();
			}
			apkpartFile.renameTo(apkFile);
		}

		// 通过Intent安装APK文件
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(Uri.parse("file://" + mSavePath + "/" + FxConst.baohe_file_name),
				"application/vnd.android.package-archive");
		startActivity(i);
		System.exit(1);
	}

	private void getGift() {
		Object tag = down.getTag();
		if ((tag instanceof Integer && ((Integer) tag).intValue() == R.string.updateLauncher) || isAppInstalled(this)) {
			giftBtn.setClickable(false);
			String giftCode = null;
			SharedPreferences sp = getSharedPreferences("hjLauncher", Context.MODE_PRIVATE);
			if (sp != null) {
				giftCode = sp.getString("giftCode", null);
			}

			if (giftCode != null) {
				ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				clip.setText(giftCode); // 复制

				String str = getResources().getString(R.string.copyGift);
				str = String.format(str, giftCode);
				Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
				giftBtn.setClickable(true);
			} else {
				new Thread(new GetGiftCodeThread(handler, MainActivity.this, udid)).start();
			}
		} else {
			Toast.makeText(this, R.string.noGift, Toast.LENGTH_LONG).show();
		}
	}

	private void installApk() {
		down.setClickable(true);
		if (isAppInstalled(this)) {
			down.setBackgroundResource(R.drawable.button_lvup_01);
			down.setTag(FxConst.update_tag);
		} else {
			// 通过Intent安装APK文件
			String mSavePath = Environment.getExternalStorageDirectory() + "/download";
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setDataAndType(Uri.parse("file://" + mSavePath + "/" + FxConst.xj_file_name),
					"application/vnd.android.package-archive");
			startActivity(i);
		}
	}

	// private void launch() {
	// ComponentName cn = new ComponentName(xj_pkg_name, xj_class_name);
	// Intent i = new Intent();
	// i.setComponent(cn);
	// startActivityForResult(i, RESULT_OK);
	// }

	private void showRedownload() {
		try {
			progressBtn.setImageBitmap(null);
			textPercent.setText("");

			// String mSavePath = Environment.getExternalStorageDirectory() + "/download";
			// File apkpartFile = new File(mSavePath, xj_file_name + "part");
			// if (apkpartFile.exists()) {
			// apkpartFile.delete();
			// }

			down.setBackgroundResource(R.drawable.button_download_03);
			down.setTag(FxConst.down_tag);
			down.setClickable(true);
			hideLoading();

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(android.R.string.dialog_alert_title).setMessage(R.string.downFail)
					.setIcon(android.R.drawable.ic_dialog_info).setCancelable(false)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							download();
						}
					}).setNegativeButton(android.R.string.cancel, null).show();
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
		}
	}

	private void showReUpdate() {
		try {
			if (progressDialog != null) {
				progressDialog.hide();
			}

			String mSavePath = Environment.getExternalStorageDirectory() + "/download";
			File apkpartFile = new File(mSavePath, FxConst.baohe_file_name + "part");
			if (apkpartFile.exists()) {
				apkpartFile.delete();
			}

			down.setBackgroundResource(R.drawable.button_download_03);
			down.setClickable(true);
			hideLoading();

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(android.R.string.dialog_alert_title).setMessage(R.string.updateFail)
					.setIcon(android.R.drawable.ic_dialog_info).setCancelable(false)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							updateLauncher();
						}
					}).setNegativeButton(android.R.string.cancel, null).show();
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
		}
	}

	private void showStorageError() {
		try {
			down.setClickable(true);
			hideLoading();

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(android.R.string.dialog_alert_title).setMessage(R.string.storageNeeded)
					.setIcon(android.R.drawable.ic_dialog_info).setCancelable(false)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					}).show();
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
		}
	}

	private void updateLauncher() {
		try {
			down.setClickable(true);
			if (isWifi(this)) {
				startUpdate();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(android.R.string.dialog_alert_title).setMessage(R.string.mobile)
						.setIcon(android.R.drawable.ic_dialog_info).setCancelable(false)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startUpdate();
							}
						}).setNegativeButton(android.R.string.cancel, null).show();
			}
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
		}
	}

	private void startUpdate() {
		try {
			// 创建ProgressDialog对象
			progressDialog = new ProgressDialog(MainActivity.this);
			// 设置进度条风格，风格为长形，有刻度的
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			// 设置ProgressDialog 标题
			progressDialog.setTitle("正在下载更新包，请稍候");
			// 设置ProgressDialog 提示信息
			// progressDialog.setMessage("正在下载更新包，请稍候");
			// 设置ProgressDialog 标题图标
			progressDialog.setIcon(android.R.drawable.ic_dialog_info);
			// 设置ProgressDialog 的取消按钮
			progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
					getText(android.R.string.cancel),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							UpdateApkThread.stop();
							progressDialog.hide();
						}
					});
			// 设置ProgressDialog 的进度条是否不明确
			progressDialog.setIndeterminate(false);
			// 设置ProgressDialog 是否可以按退回按键取消
			progressDialog.setCancelable(false);
			// 设置ProgressDialog 进度条进度
			progressDialog.setProgress(0);
			// 让ProgressDialog显示
			progressDialog.show();

			new Thread(new UpdateApkThread(handler, MainActivity.this)).start();
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
		}
	}

	private void refreshProgress(int progress) {
		textPercent.setText(progress + "%");
		progress += 5;
		Bitmap bm2 = Bitmap.createBitmap(bm, 0, 0, Math.round(bm.getWidth() * progress / 118), bm.getHeight());
		progressBtn.setImageBitmap(bm2);
	}

	private void refreshUpdateProgress(int progress) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.setProgress(progress);
		}
	}

	private void left() {
		try {
			// 图片渐变模糊度始终
			AlphaAnimation aa1 = new AlphaAnimation(1.0f, 0.0f);
			// 渐变时间
			aa1.setDuration(1000);
			// 展示图片渐变动画
			root.startAnimation(aa1);
			aa1.setAnimationListener(new AnimationListener() {
				public void onAnimationStart(Animation animation) {
				}

				public void onAnimationEnd(Animation animation) {
					// 图片渐变模糊度始终
					AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
					// 渐变时间
					aa.setDuration(1500);

					Object tag1 = root.getTag();
					if (!(tag1 instanceof Integer)) {
						tag1 = R.drawable.bg;
					}
					switch ((Integer) tag1) {
						case R.drawable.bg:
							root.setTag(R.drawable.manhua_11);
							root.setBackgroundResource(R.drawable.manhua_11);
							break;
						case R.drawable.manhua_1:
							root.setTag(R.drawable.bg);
							root.setBackgroundResource(R.drawable.bg);
							break;
						case R.drawable.manhua_2:
							root.setTag(R.drawable.manhua_1);
							root.setBackgroundResource(R.drawable.manhua_1);
							break;
						case R.drawable.manhua_3:
							root.setTag(R.drawable.manhua_2);
							root.setBackgroundResource(R.drawable.manhua_2);
							break;
						case R.drawable.manhua_4:
							root.setTag(R.drawable.manhua_3);
							root.setBackgroundResource(R.drawable.manhua_3);
							break;
						case R.drawable.manhua_5:
							root.setTag(R.drawable.manhua_4);
							root.setBackgroundResource(R.drawable.manhua_4);
							break;
						case R.drawable.manhua_6:
							root.setTag(R.drawable.manhua_5);
							root.setBackgroundResource(R.drawable.manhua_5);
							break;
						case R.drawable.manhua_7:
							root.setTag(R.drawable.manhua_6);
							root.setBackgroundResource(R.drawable.manhua_6);
							break;
						case R.drawable.manhua_8:
							root.setTag(R.drawable.manhua_7);
							root.setBackgroundResource(R.drawable.manhua_7);
							break;
						case R.drawable.manhua_9:
							root.setTag(R.drawable.manhua_8);
							root.setBackgroundResource(R.drawable.manhua_8);
							break;
						case R.drawable.manhua_10:
							root.setTag(R.drawable.manhua_9);
							root.setBackgroundResource(R.drawable.manhua_9);
							break;
						case R.drawable.manhua_11:
							root.setTag(R.drawable.manhua_10);
							root.setBackgroundResource(R.drawable.manhua_10);
							break;
						default:
							break;
					}
					// 展示图片渐变动画
					root.startAnimation(aa);
				}

				public void onAnimationRepeat(Animation animation) {
				}
			});
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
		}
	}

	private void right() {
		try {
			// 图片渐变模糊度始终
			AlphaAnimation aa1 = new AlphaAnimation(1.0f, 0.0f);
			// 渐变时间
			aa1.setDuration(1000);
			// 展示图片渐变动画
			root.startAnimation(aa1);
			aa1.setAnimationListener(new AnimationListener() {
				public void onAnimationStart(Animation animation) {
				}

				public void onAnimationEnd(Animation animation) {
					// 图片渐变模糊度始终
					AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
					// 渐变时间
					aa.setDuration(1500);

					Object tag1 = root.getTag();
					if (!(tag1 instanceof Integer)) {
						tag1 = R.drawable.bg;
					}
					switch ((Integer) tag1) {
						case R.drawable.bg:
							root.setTag(R.drawable.manhua_1);
							root.setBackgroundResource(R.drawable.manhua_1);
							break;
						case R.drawable.manhua_1:
							root.setTag(R.drawable.manhua_2);
							root.setBackgroundResource(R.drawable.manhua_2);
							break;
						case R.drawable.manhua_2:
							root.setTag(R.drawable.manhua_3);
							root.setBackgroundResource(R.drawable.manhua_3);
							break;
						case R.drawable.manhua_3:
							root.setTag(R.drawable.manhua_4);
							root.setBackgroundResource(R.drawable.manhua_4);
							break;
						case R.drawable.manhua_4:
							root.setTag(R.drawable.manhua_5);
							root.setBackgroundResource(R.drawable.manhua_5);
							break;
						case R.drawable.manhua_5:
							root.setTag(R.drawable.manhua_6);
							root.setBackgroundResource(R.drawable.manhua_6);
							break;
						case R.drawable.manhua_6:
							root.setTag(R.drawable.manhua_7);
							root.setBackgroundResource(R.drawable.manhua_7);
							break;
						case R.drawable.manhua_7:
							root.setTag(R.drawable.manhua_8);
							root.setBackgroundResource(R.drawable.manhua_8);
							break;
						case R.drawable.manhua_8:
							root.setTag(R.drawable.manhua_9);
							root.setBackgroundResource(R.drawable.manhua_9);
							break;
						case R.drawable.manhua_9:
							root.setTag(R.drawable.manhua_10);
							root.setBackgroundResource(R.drawable.manhua_10);
							break;
						case R.drawable.manhua_10:
							root.setTag(R.drawable.manhua_11);
							root.setBackgroundResource(R.drawable.manhua_11);
							break;
						case R.drawable.manhua_11:
							root.setTag(R.drawable.bg);
							root.setBackgroundResource(R.drawable.bg);
							break;
						default:
							break;
					}
					// 展示图片渐变动画
					root.startAnimation(aa);
				}

				public void onAnimationRepeat(Animation animation) {
				}
			});
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
		}
	}

	// public static String httpGet(String url, String encode) {
	// try {
	// Log.i(TAG, "url: \t" + url);
	// SSLSocketFactory.getSocketFactory().setHostnameVerifier(new AllowAllHostnameVerifier()); // allow all https
	// HttpClient client = new DefaultHttpClient();
	// client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000); // 请求超时
	// client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000); // 读取超时
	//
	// HttpGet get = new HttpGet(url);
	// HttpResponse response = client.execute(get);
	// if (response.getStatusLine().getStatusCode() == 200) {
	// String strResult = EntityUtils.toString(response.getEntity(), encode);
	// Log.i(TAG, "get return \t : " + strResult);
	// return strResult;
	// }
	// } catch (Exception e) {
	// Log.e(TAG, e.toString(), e);
	// }
	// return null;
	// }

	// public static String httpPost(String url, Map<String, String> param, String encode) {
	// try {
	// SSLSocketFactory.getSocketFactory().setHostnameVerifier(new AllowAllHostnameVerifier()); // allow all https
	// List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	// StringBuilder sb = new StringBuilder();
	// if (param != null) {
	// for (Entry<String, String> entry : param.entrySet()) {
	// if (entry.getValue() != null && entry.getValue().length() > 0) {
	// nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
	// sb.append(entry.getKey()).append(":").append(entry.getValue()).append("\t");
	// }
	// }
	// }
	// Log.i(TAG, url + "\t : " + sb.toString());
	//
	// DefaultHttpClient client = new DefaultHttpClient();
	// client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000); // 请求超时
	// client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000); // 读取超时
	// client.setRedirectHandler(new DefaultRedirectHandler() {
	// @Override
	// public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
	// return false; // no redirect
	// // boolean isRedirect = super.isRedirectRequested(response, context);
	// // if (!isRedirect) {
	// // int responseCode = response.getStatusLine().getStatusCode();
	// // if (responseCode == 301 || responseCode == 302) {
	// // return true;
	// // }
	// // }
	// // return isRedirect;
	// }
	// });
	//
	// HttpPost httpPost = new HttpPost(url);
	// httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=" + encode);
	// httpPost.setEntity(new UrlEncodedFormEntity(nvps, encode));
	// HttpResponse response = client.execute(httpPost);
	//
	// Log.i(TAG, "post return code \t : " + response.getStatusLine().getStatusCode());
	// if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
	// String strResult = EntityUtils.toString(response.getEntity(), encode);
	// Log.i(TAG, "post return \t : " + strResult);
	// return strResult;
	// } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY
	// || response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
	// Header locationHeader = response.getFirstHeader("location");
	// if (locationHeader != null) {
	// String location = locationHeader.getValue();
	// Log.i(TAG, "post redirect to \t : " + location);
	// return httpPost(location, param, encode);
	// }
	// }
	// } catch (Exception e) {
	// Log.e(TAG, e.toString(), e);
	// }
	// return null;
	// }

	public static JSONObject parseResult2Json(String ret) {
		try {
			if (ret != null && ret.length() > 0) {
				JSONObject joRes = new JSONObject(ret);
				if (joRes != null && "0".equals(joRes.optString("retCode"))) {
					return joRes;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
		}
		return null;
	}

	public static boolean isBackground(Context context) {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		for (RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.processName.equals(context.getPackageName())) {
				Log.i(TAG, appProcess.processName);
				if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}
}
